package com.library.infrastructure.messaging.documentation;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║           ARQUITETURA ASSÍNCRONA ORIENTADA A EVENTOS                     ║
 * ║                  Event-Driven Architecture (EDA)                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * RESUMO DA REFATORAÇÃO
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ANTES (Síncrono com Dual Write):
 * ┌─────────────┐
 * │  BookService│
 * │  Sincronus  │
 * └──────┬──────┘
 *        │
 *        ├─→ repositoryPort.save(book)        ① PERSISTE
 *        └─→ eventPublisherPort.publish()     ② PUBLICA
 *
 * ⚠️ PROBLEMA: Se ① sucede mas ② falha → dados no banco mas não na fila
 *               Se ② sucede mas ① falha → dados na fila mas não no banco
 *               Resultado: INCONSISTÊNCIA EVENTUAL!
 *
 *
 * DEPOIS (Assíncrono Orientado a Eventos):
 * ┌─────────────┐
 * │  BookService│
 * │  Assincrono │
 * └──────┬──────┘
 *        │
 *        └─→ eventPublisherPort.publishBookCreated(book)  ← APENAS PUBLICA
 *                 │
 *                 ↓
 *        ┌─────────────────┐
 *        │   RabbitMQ      │
 *        │   Event Queue   │
 *        └────────┬────────┘
 *                 │
 *        ┌────────▼──────────┐
 *        │ BookCreatedConsumer│
 *        │   (Async)         │
 *        │ repositoryPort    │
 *        │   .save(book)     │
 *        └───────────────────┘
 *
 * ✅ BENEFÍCIO: Publisher não precisa se preocupar com persistência
 *               Consumer é 100% responsável por persistir
 *               Banco terá o livro com CONSISTÊNCIA EVENTUAL
 *
 *
 * FLUXO DETALHADO - UCA: POST /books
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * 1. CLIENTE: POST /books (HTTP Request)
 *    ┌─────────────────────────────────────────┐
 *    │ {                                       │
 *    │   "title": "Domain-Driven Design",      │
 *    │   "author": "Eric Evans",               │
 *    │   "isbn": "0321125215",                 │
 *    │   "publicationYear": 2003               │
 *    │ }                                       │
 *    └─────────────────────────────────────────┘
 *           ↓
 *
 * 2. BookController (@PostMapping)
 *    └─ @Valid CreateBookRequest (validação Bean Validation)
 *       ↓
 *
 * 3. BookService.createBook()
 *    └─ new Book() → gera UUID (domínio é responsável)
 *    └─ book.hasValidIsbn() → valida regra de negócio
 *    └─ eventPublisherPort.publishBookCreated(book)
 *       └─ NÃO SALVA NO BANCO ❌ (mudança crucial)
 *       ↓
 *
 * 4. RabbitBookEventPublisher.publishBookCreated()
 *    └─ BookCreatedEvent.from(book) → cria DTO estruturado
 *       {
 *         "eventType": "BOOK_CREATED",
 *         "eventVersion": "1.0",
 *         "timestamp": "2026-04-23T10:30:45.123Z",
 *         "bookId": "550e8400-e29b-41d4-a716-446655440000",
 *         "title": "Domain-Driven Design",
 *         "author": "Eric Evans",
 *         "isbn": "0321125215",
 *         "publicationYear": 2003
 *       }
 *    └─ objectMapper.writeValueAsString(event) → JSON robusto
 *    └─ rabbitTemplate.convertAndSend(queue, jsonMessage)
 *       ↓
 *
 * 5. RabbitMQ Queue (ARMAZENA)
 *    └─ Mensagem fica na fila até ser consumida
 *    └─ Fila é PERSISTENTE (durable=true)
 *       ↓
 *
 * 6. BookController → ResponseEntity.accepted()
 *    └─ HTTP 202 Accepted (NÃO 201 Created!)
 *    └─ Header: Location: /books/550e8400-e29b-41d4-a716-446655440000
 *    └─ Body: BookResponse (livro gerado, mas SEM persistir ainda)
 *    └─ Cliente recebe: "Seu request foi aceito, será processado assincronamente"
 *       ↓
 *
 * 7. ⏳ PROCESSAMENTO ASSÍNCRONO (ocorre depois, sem bloquear o cliente)
 *    └─ Spring AMQP ouve a fila continuamente
 *       ↓
 *
 * 8. BookCreatedConsumer.consumeBookCreatedEvent()
 *    └─ @RabbitListener recebe a mensagem JSON
 *    └─ objectMapper.readValue(json) → BookCreatedEvent
 *    └─ Reconstrói Book(bookId, title, author, isbn, publicationYear)
 *    └─ bookRepositoryPort.save(book) → PERSISTE AQUI ✅
 *       └─ INSERT INTO books (id, title, author, isbn, publication_year) VALUES (...)
 *       ↓
 *
 * 9. ✅ SUCESSO
 *    └─ Spring AMQP faz ACK da mensagem na fila
 *    └─ Mensagem é removida (processada)
 *    └─ Livro está agora no banco H2
 *       ↓
 *
 * 10. CLIENTE: GET /books/{id}
 *     └─ Busca o livro (pode ser imediato, ou depois, dependendo do delay)
 *     └─ BookService.findBookById() → busca no repositório
 *     └─ Retorna 200 OK com o livro (consistência eventual atingida)
 *
 *
 * TRATAMENTO DE ERRO ASSÍNCRONO
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Se BookCreatedConsumer.consumeBookCreatedEvent() lança Exception:
 *
 * 1️⃣ PRIMEIRA TENTATIVA FALHA
 *    ├─ Logger: ❌ [Consumer] Falha ao processar evento
 *    ├─ Exceção propagada
 *    └─ Spring AMQP faz NACK (não confirma entrega)
 *       ↓
 *
 * 2️⃣ RETRY (configurado em application.properties)
 *    ├─ Mensagem volta à fila
 *    ├─ Spring aguarda intervalo de espera
 *    └─ Tenta novamente
 *       ↓
 *
 * 3️⃣ APÓS MÚLTIPLAS TENTATIVAS (ex: 3 tentativas)
 *    ├─ Se continua falhando
 *    └─ Mensagem vai para DEAD LETTER QUEUE (DLQ) [FUTURO]
 *       └─ DLQ é uma fila de erro para investigação manual
 *
 *
 * COMPARAÇÃO: SÍNCRONO vs ASSÍNCRONO
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ┌──────────────────┬────────────────┬──────────────────┐
 * │    Aspecto       │   SÍNCRONO     │   ASSÍNCRONO     │
 * ├──────────────────┼────────────────┼──────────────────┤
 * │ Latência         │ ~50-100ms      │ ~202ms (aceitado)│
 * │ Bloqueio         │ Cliente espera │ Cliente não      │
 * │                  │   persistência │   espera         │
 * │ Dual Write       │ SIM ⚠️         │ NÃO ✅           │
 * │ Responsabilidade │ BookService faz│ Consumer persiste│
 * │ Consistência     │ Imediata       │ Eventual         │
 * │ Escalabilidade   │ Limitada       │ Maior (fila)     │
 * │ Monitoramento    │ Simples        │ Mais complexo    │
 * │ DLQ              │ N/A            │ Suportado ✅     │
 * └──────────────────┴────────────────┴──────────────────┘
 *
 *
 * CÓDIGO-CHAVE ANTES vs DEPOIS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ❌ ANTES (BookService.createBook) — Síncrono:
 * ──────────────────────────────────────────────
 * var book = new Book(title, author, isbn, publicationYear);
 * var savedBook = repositoryPort.save(book);           // ① AQUI PERSISTE
 * eventPublisherPort.publishBookCreated(savedBook);    // ② DEPOIS PUBLICA
 * return savedBook;
 * // Problema: Se ① OK mas ② falha → inconsistência
 *
 *
 * ✅ DEPOIS (BookService.createBook) — Assíncrono:
 * ──────────────────────────────────────────────
 * var book = new Book(title, author, isbn, publicationYear);
 * // ❌ NÃO SALVA AQUI
 * eventPublisherPort.publishBookCreated(book);        // ① APENAS PUBLICA
 * return book;                                         // ② Retorna imediatamente
 * // Benefício: Consumer (async) persiste depois
 *             // Se publica OK → Consumer persistirá (guarantia de ordem)
 *             // Sem dual write!
 *
 *
 * ❌ ANTES (BookController) — HTTP 201 Created:
 * ────────────────────────────────────────────
 * Book created = bookUseCase.createBook(...);
 * return ResponseEntity.created(location).body(toResponse(created));
 * // HTTP 201 sugere que recurso está PRONTO no servidor
 * // MAS: com assincronismo, não está!
 *
 *
 * ✅ DEPOIS (BookController) — HTTP 202 Accepted:
 * ────────────────────────────────────────────
 * Book created = bookUseCase.createBook(...);
 * return ResponseEntity.accepted()
 *                      .location(location)
 *                      .body(toResponse(created));
 * // HTTP 202 comunica: "Seu request foi ACEITO, processando assincronamente"
 * // Cliente sabe que deve fazer GET depois para validar persistência
 *
 *
 * ESTRUTURA DOS EVENTOS - DTOs (NEW)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Arquivo: BookEventDTOs.java (Nova classe)
 * Camada:  Infrastructure/Messaging/Event
 *
 * public record BookCreatedEvent(
 *     String eventType,        // "BOOK_CREATED" (permite tipos diferentes)
 *     String eventVersion,     // "1.0" (evolução do schema sem quebra)
 *     Instant timestamp,       // Quando foi gerado
 *     UUID bookId,             // ID do livro
 *     String title,
 *     String author,
 *     String isbn,
 *     int publicationYear
 * )
 *
 * Benefícios:
 * • Estrutura clara e versionada
 * • Jackson serializa/desserializa automaticamente
 * • Factory method: BookCreatedEvent.from(book)
 * • Type-safe (não é String manual)
 * • Pronto para evolução (adicionar campos sem quebra)
 *
 *
 * CONFIGURAÇÃO NECESSÁRIA (application.properties)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * # RabbitMQ
 * spring.rabbitmq.host=localhost
 * spring.rabbitmq.port=5672
 * spring.rabbitmq.username=guest
 * spring.rabbitmq.password=guest
 *
 * # Queue config
 * app.messaging.book-created-queue=book-created-queue
 *
 * # (Opcional) Retry policy para @RabbitListener
 * spring.rabbitmq.listener.simple.retry.enabled=true
 * spring.rabbitmq.listener.simple.retry.max-attempts=3
 * spring.rabbitmq.listener.simple.retry.initial-interval=1000
 * spring.rabbitmq.listener.simple.retry.multiplier=1.0
 * spring.rabbitmq.listener.simple.retry.max-interval=10000
 *
 *
 * PRÓXIMAS EVOLUÇÕES (ROADMAP)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * 1. Dead Letter Queue (DLQ)
 *    └─ Fila para mensagens que falharam após retries
 *    └─ Implementar BookEventFailedConsumer
 *    └─ Monitoramento manual de falhas
 *
 * 2. Outbox Pattern
 *    └─ Tabela outbox_events no banco
 *    └─ Consumer separado lê outbox e publica em RabbitMQ
 *    └─ Garante que evento sempre será publicado (se persistir OK)
 *
 * 3. Event Sourcing
 *    └─ Armazenar TODO evento (não apenas estado final)
 *    └─ Auditoria completa de mudanças
 *
 * 4. CQRS (Command Query Responsibility Segregation)
 *    └─ Separate models para leitura e escrita
 *    └─ Projeções em cache/denormalizado para queries rápidas
 *
 * 5. Status Tracking
 *    └─ GET /books/{id}/status → retorna estado do processamento
 *    └─ Cliente monitora se livro foi persistido
 *
 *
 * VANTAGENS DA ARQUITETURA FINAL
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ✅ Zero Dual Write Problem
 *    └─ Publisher não persiste → sem inconsistência
 *
 * ✅ Escalabilidade
 *    └─ Fila absorve picos de carga
 *    └─ Consumers podem ser escalados horizontalmente
 *
 * ✅ Resiliência
 *    └─ Se Consumer falha → retry automático
 *    └─ Se banco cai → fila mantém mensagens (persistente)
 *
 * ✅ Desacoplamento
 *    └─ Publisher não conhece Consumer
 *    └─ Fácil adicionar novos consumers (auditoria, cache, etc)
 *
 * ✅ Rastreabilidade
 *    └─ Cada evento tem timestamp
 *    └─ Logs detalhados de processamento
 *
 * ✅ Type-Safety
 *    └─ DTOs estruturados (não Strings)
 *    └─ Jackson garante serialização correta
 *
 * ✅ Arquitetura Hexagonal Mantida
 *    └─ Domínio limpo (Book + portas)
 *    └─ Infra implementa adapters
 *    └─ Sem acoplamento com RabbitMQ no domínio
 *
 *
 * COMO TESTAR
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * 1. Teste de Integração (BookControllerIntegrationTest)
 *    └─ POST /books
 *    └─ Validar HTTP 202 Accepted (antes era 201)
 *    └─ Validar Location header
 *
 * 2. Teste do Consumer (BookCreatedConsumerTest)
 *    └─ Mock ObjectMapper
 *    └─ Mock BookRepositoryPort
 *    └─ Invocar consumeBookCreatedEvent() com JSON válido
 *    └─ Verificar se save() foi chamado
 *
 * 3. Teste E2E (com RabbitMQ real)
 *    └─ POST /books
 *    └─ Aguardar alguns segundos
 *    └─ GET /books/{id}
 *    └─ Validar que livro foi persistido
 *
 * 4. Teste de Erro
 *    └─ Mock BookRepositoryPort.save() para lançar exceção
 *    └─ Invocar consumer
 *    └─ Validar que exceção foi propagada (para retry)
 *
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * FIM DA DOCUMENTAÇÃO
 * ═══════════════════════════════════════════════════════════════════════════
 */
public final class AsyncEventDrivenArchitectureDoc {
    private AsyncEventDrivenArchitectureDoc() {}
}
