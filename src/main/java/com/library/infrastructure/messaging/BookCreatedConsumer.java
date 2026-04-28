package com.library.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.domain.model.Book;
import com.library.domain.port.BookRepositoryPort;
import com.library.infrastructure.messaging.event.BookEventDTOs.BookCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║ CAMADA: Infrastructure — Adapter de Mensageria (Consumer) ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║ CONCEITOS APLICADOS: ║
 * ║ • Assincronismo: Consumer recebe evento APÓS publisher ║
 * ║ • Responsabilidade: Consumer PERSISTE no banco (não publisher)║
 * ║ • Desacoplamento: Publisher não sabe se persistiu com sucesso ║
 * ║ • Consistência Eventual: dados chegam ao banco com delay ║
 * ║ • Serialização: ObjectMapper desserializa JSON estruturado ║
 * ║ • Tratamento de Erro: @RabbitListener com manual ack ║
 * ║ • Retry: Spring AMQP reentrega msg em caso de exceção ║
 * ║ • Preparação: Estrutura pronta para futura DLQ ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Component
public class BookCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookCreatedConsumer.class);

    private final BookRepositoryPort bookRepositoryPort;
    private final ObjectMapper objectMapper;

    public BookCreatedConsumer(BookRepositoryPort bookRepositoryPort, ObjectMapper objectMapper) {
        this.bookRepositoryPort = bookRepositoryPort;
        this.objectMapper = objectMapper;
    }

    /**
     * Consome eventos BookCreatedEvent da fila.
     *
     * Fluxo:
     * 1. Spring AMQP entrega a mensagem (JSON) para este método
     * 2. Desserializa em BookCreatedEvent usando ObjectMapper
     * 3. Reconstrói a entidade Book do domínio
     * 4. Persiste usando BookRepositoryPort
     * 5. Se sucesso: Spring faz ACK automático na mensagem
     * 6. Se erro: Spring faz NACK e reentrega (retry policy)
     *
     * Nota: Seria possível adicionar anotação @Retryable para controlar
     * o comportamento de retry e levar mensagens com falha final para DLQ.
     *
     * @param message JSON serializado como String
     */
    @RabbitListener(queues = "${app.messaging.book-created-queue}")
    public void consumeBookCreatedEvent(String message) {
        log.info("📨 [Consumer] Mensagem recebida da fila: {}", message);

        try {
            // 1. Desserializa JSON → BookCreatedEvent
            BookCreatedEvent event = objectMapper.readValue(message, BookCreatedEvent.class);

            log.debug(
                    "📦 [Consumer] Evento desserializado: eventType={}, bookId={}, title={}",
                    event.eventType(),
                    event.bookId(),
                    event.title());

            // 2. Reconstrói a entidade de domínio a partir do evento
            Book book = new Book(
                    event.bookId(),
                    event.title(),
                    event.author(),
                    event.isbn(),
                    event.publicationYear());

            // 3. PERSISTE no banco usando a porta
            // Esta é a responsabilidade EXCLUSIVA do Consumer!
            bookRepositoryPort.save(book);

            log.info(
                    "✅ [Consumer] Livro persistido com sucesso: id={}, title={}",
                    book.getId(),
                    book.getTitle());

        } catch (Exception e) {
            log.error(
                    "❌ [Consumer] Falha ao processar evento: {} | Mensagem original: {}",
                    e.getMessage(),
                    message,
                    e);

            // Propagar exceção para Spring fazer NACK e retry
            // Em produção: usar @Retryable + BackOff para controlar tentativas
            // Após N tentativas: enviar para DLQ (Dead Letter Queue)
            throw new RuntimeException("Falha ao processar BookCreatedEvent", e);
        }
    }
}
