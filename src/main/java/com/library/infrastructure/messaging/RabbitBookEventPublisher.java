package com.library.infrastructure.messaging;

import com.library.domain.model.Book;
import com.library.domain.port.BookEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Infrastructure — Adapter de Mensageria (RabbitMQ)   ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Arquitetura Hexagonal: "Driven Adapter" de mensageria —   ║
 * ║    implementa BookEventPublisherPort sem que o domínio        ║
 * ║    saiba que existe RabbitMQ                                 ║
 * ║  • SOLID / SRP: única responsabilidade — publicar mensagem    ║
 * ║  • SOLID / DIP: domínio chama a interface; Spring injeta este ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Component
public class RabbitBookEventPublisher implements BookEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitBookEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    // Valor injetado do application.properties — evita "magic strings" no código
    @Value("${app.messaging.book-created-queue}")
    private String bookCreatedQueue;

    public RabbitBookEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishBookCreated(Book book) {
        // Monta payload como String JSON simples (sem biblioteca externa)
        // Em produção: use Jackson ObjectMapper ou um DTO de evento dedicado
        String message = String.format(
                "{\"eventType\":\"BOOK_CREATED\",\"bookId\":\"%s\",\"title\":\"%s\",\"author\":\"%s\"}",
                book.getId(),
                book.getTitle(),
                book.getAuthor()
        );

        try {
            /*
             * RabbitTemplate.convertAndSend(queue, message):
             * — Usa o default exchange ("") com routing key = nome da fila
             * — Em produção: configure Exchange + RoutingKey separados
             */
            rabbitTemplate.convertAndSend(bookCreatedQueue, message);
            log.info("Evento publicado na fila '{}': bookId={}", bookCreatedQueue, book.getId());
        } catch (Exception e) {
            // Em produção: use dead-letter queue, circuit breaker ou outbox pattern
            log.error("Falha ao publicar evento para bookId={}: {}", book.getId(), e.getMessage());
        }
    }
}
