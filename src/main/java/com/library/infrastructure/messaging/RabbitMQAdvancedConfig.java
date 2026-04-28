package com.library.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Infrastructure — Configuração Avançada RabbitMQ     ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Exchange + Queue + RoutingKey (modelo producer/consumer)  ║
 * ║  • Dead Letter Queue (DLQ) para falhas crônicas              ║
 * ║  • Retry Policy via x-dead-letter-exchange                   ║
 * ║  • Configuração separada de main vs DLQ                      ║
 * ║  • Preparação para Outbox Pattern                            ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * NOTA: Esta é a configuração principal e única do RabbitMQ.
 * Inclui Exchange, Queue, DLQ e Bindings.
 */
@Configuration
public class RabbitMQAdvancedConfig {

    // ── Valores do application.properties ──
    @Value("${app.messaging.book-created-queue:book-created-queue}")
    private String bookCreatedQueue;

    @Value("${app.messaging.book-created-exchange:book-events-exchange}")
    private String bookEventExchange;

    @Value("${app.messaging.book-created-routing-key:book.created}")
    private String bookCreatedRoutingKey;

    // ── Dead Letter Queue (DLQ) ──
    @Value("${app.messaging.book-created-dlq:book-created-dlq}")
    private String bookCreatedDLQ;

    @Value("${app.messaging.book-created-dlx:book-created-dlx}")
    private String bookCreatedDLX;

    // ═══════════════════════════════════════════════════════════════
    // EXCHANGE (Roteador de mensagens)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Exchange tipo Direct: mensagens vão para filas baseado em routing key
     *
     * Alternativas:
     * - ExchangeTypes.TOPIC: routing key com wildcard (#, *)
     * - ExchangeTypes.FANOUT: broadcast para todas as filas
     * - ExchangeTypes.HEADERS: baseado em headers HTTP-like
     */
    @Bean
    public DirectExchange bookEventExchange() {
        return new DirectExchange(
                bookEventExchange,
                true,    // durable: sobrevive restart do RabbitMQ
                false    // autoDelete: não apaga quando sem consumers
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // MAIN QUEUE (Fila de processamento normal)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Fila principal para eventos BookCreated.
     *
     * Argumentos:
     * - durable: true → mensagens persistem em disco
     * - x-dead-letter-exchange: encaminha para DLX após max-retries
     * - x-message-ttl: (opcional) TTL por mensagem em ms
     * - x-max-length: (opcional) limite de mensagens na fila
     */
    @Bean
    public Queue bookCreatedQueue() {
        return QueueBuilder.durable(bookCreatedQueue)
                // ↓ Configuração de Dead Letter Exchange (para retries esgotados)
                .withArgument("x-dead-letter-exchange", bookCreatedDLX)
                .withArgument("x-dead-letter-routing-key", bookCreatedRoutingKey + ".dead-letter")
                // ↓ (Opcional) TTL: mensagem expira após 24h se não consumida
                .withArgument("x-message-ttl", 86400000) // 24 horas em ms
                // ↓ (Opcional) Max length: máximo 100k mensagens
                .withArgument("x-max-length", 100000)
                .build();
    }

    /**
     * Binding: conecta Queue ao Exchange com Routing Key
     * Quando uma mensagem chega no exchange com routing key "book.created",
     * ela é roteada para a fila "book-created-queue"
     */
    @Bean
    public Binding bookCreatedBinding(
            Queue bookCreatedQueue,
            DirectExchange bookEventExchange) {
        return BindingBuilder
                .bind(bookCreatedQueue)
                .to(bookEventExchange)
                .with(bookCreatedRoutingKey);
    }

    // ═══════════════════════════════════════════════════════════════
    // DEAD LETTER EXCHANGE (DLX) + DEAD LETTER QUEUE (DLQ)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Exchange para mensagens que falharam
     * Estas são mensagens que:
     * - Falharam após todas as tentativas de retry
     * - Foram rejeitadas explicitamente (nacked)
     * - Expiraram (TTL)
     */
    @Bean
    public DirectExchange bookCreatedDLX() {
        return new DirectExchange(
                bookCreatedDLX,
                true,    // durable
                false    // autoDelete
        );
    }

    /**
     * Fila de Dead Letter: armazena mensagens problemáticas
     * Um consumer separado pode monitorar esta fila e:
     * - Logar detalhes da falha
     * - Alertar operacional
     * - Tentar reprocessar manualmente
     */
    @Bean
    public Queue bookCreatedDLQ() {
        return QueueBuilder.durable(bookCreatedDLQ)
                // ↓ (Opcional) TTL mais longo na DLQ: 7 dias
                .withArgument("x-message-ttl", 604800000) // 7 dias
                // ↓ (Opcional) Manter histórico menor: max 10k
                .withArgument("x-max-length", 10000)
                .build();
    }

    /**
     * Binding para DLX + DLQ
     */
    @Bean
    public Binding bookCreatedDLQBinding(
            Queue bookCreatedDLQ,
            DirectExchange bookCreatedDLX) {
        return BindingBuilder
                .bind(bookCreatedDLQ)
                .to(bookCreatedDLX)
                .with(bookCreatedRoutingKey + ".dead-letter");
    }

    // ═══════════════════════════════════════════════════════════════
    // COMENTÁRIO: COMO ISSO FUNCIONA (Retry Flow)
    // ═══════════════════════════════════════════════════════════════

    /*
     * CENÁRIO: Consumer falha 3 vezes
     *
     * 1️⃣ Mensagem chega no Exchange: book-events-exchange
     *    └─ Routing key: "book.created"
     *       ↓
     *
     * 2️⃣ Exchange roteia para Queue: book-created-queue
     *    └─ Consumer (@RabbitListener) recebe
     *       ↓
     *
     * 3️⃣ Consumer.consumeBookCreatedEvent() é invocado
     *    ├─ Tenta persitir no banco
     *    ├─ Falha! Lança RuntimeException
     *       ↓
     *
     * 4️⃣ Spring AMQP vê a exceção
     *    ├─ Spring.rabbitmq.listener.simple.retry.enabled=true
     *    ├─ Aguarda 1 segundo (initial-interval)
     *    ├─ Retorna mensagem à fila
     *    └─ Tenta novamente (até 3 vezes)
     *       ↓
     *
     * 5️⃣ Após 3 tentativas sem sucesso
     *    ├─ Spring AMQP faz NACK final
     *    ├─ Mensagem é roteada para Dead Letter Exchange (DLX)
     *    │  └─ Routing key muda para "book.created.dead-letter"
     *       ↓
     *
     * 6️⃣ DLX roteia para DLQ (book-created-dlq)
     *    ├─ Mensagem fica armazenada para investigação
     *    └─ Um operador pode revisar e tentar reprocessar
     *
     * BENEFÍCIO: Não perde mensagens, apenas as que realmente falharam
     */

    // ═══════════════════════════════════════════════════════════════
    // OUTBOX PATTERN (Preparação para Futuro)
    // ═══════════════════════════════════════════════════════════════

    /*
     * Outbox Pattern é a evolução para garantir:
     * "Se a transação de persistência sucede, o evento SEMPRE será publicado"
     *
     * Implementação:
     * 1. BookService.createBook() salva Book E BookEvent na mesma transação
     *    ├─ INSERT INTO books (...)
     *    └─ INSERT INTO outbox_events (...)
     *
     * 2. Consumer separado (OutboxPoller) lê outbox_events
     *    ├─ SELECT * FROM outbox_events WHERE processed = false
     *    └─ Publica no RabbitMQ via BookEventPublisher
     *
     * 3. Uma vez publicado, marca como processado
     *    └─ UPDATE outbox_events SET processed = true WHERE id = ...
     *
     * BENEFÍCIO: Se RabbitMQ cai, outbox garante reenvio
     *            Se banco cai, evento ainda está no outbox
     *
     * TODO: Implementar quando necessário (classe OutboxConsumer, tabela outbox_events)
     */
}
