package com.library.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.domain.model.Book;
import com.library.domain.port.BookEventPublisherPort;
import com.library.infrastructure.messaging.event.BookEventDTOs.BookCreatedEvent;
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
 * ║  • SOLID / SRP: única responsabilidade — publicar evento      ║
 * ║  • SOLID / DIP: domínio chama a interface; Spring injeta este ║
 * ║  • Estrutura Assíncrona: publica DTOs de evento estruturados  ║
 * ║    (não JSON manual), permitindo evolução sem quebrar         ║
 * ║  • Serialização: ObjectMapper padroniza JSON                  ║
 * ║  • Logging: rastreabilidade de sucesso/falha                  ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Component
public class RabbitBookEventPublisher implements BookEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitBookEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // Valor injetado do application.properties — evita "magic strings" no código
    @Value("${app.messaging.book-created-queue}")
    private String bookCreatedQueue;

    public RabbitBookEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishBookCreated(Book book) {
        try {
            // 1. Cria o evento estruturado usando o factory method do DTO
            BookCreatedEvent event = BookCreatedEvent.from(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getIsbn(),
                    book.getPublicationYear()
            );

            // 2. Serializa para JSON usando ObjectMapper (mais robusto que String.format)
            String message = objectMapper.writeValueAsString(event);

            // 3. Publica na fila
            // RabbitTemplate.convertAndSend(queue, message):
            // — Usa o default exchange ("") com routing key = nome da fila
            // — Em produção: configure Exchange + RoutingKey separados
            rabbitTemplate.convertAndSend(bookCreatedQueue, message);

            log.info(
                    "Evento publicado na fila '{}': eventType=BOOK_CREATED, bookId={}, timestamp={}",
                    bookCreatedQueue,
                    book.getId(),
                    System.currentTimeMillis()
            );

        } catch (Exception e) {
            // Em produção: circuit breaker, retry policy ou outbox pattern
            // DLQ será implementado como evolução futura
            log.error(
                    "Falha ao publicar evento BookCreatedEvent para bookId={}: {} | Message: {}",
                    book.getId(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );

            // Propagar exceção para que o Spring traga a stack completa
            // (em produção, usar @Retryable ou Outbox Pattern)
            throw new RuntimeException("Falha ao publicar evento de criação de livro", e);
        }
    }
}
