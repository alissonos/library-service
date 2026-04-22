package com.library.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.domain.model.Book;
import com.library.domain.port.BookRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║ CAMADA: Infrastructure — Adapter de Mensageria (Consumer) ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║ CONCEITOS APLICADOS: ║
 * ║ • Consumidor RabbitMQ escutando a fila configurada. ║
 * ║ • Conversão de payload JSON para entidade de domínio. ║
 * ║ • Tratamento de falhas para acionar o Retry do Spring. ║
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

    private static int attempt = 0;

    @RabbitListener(queues = "${app.messaging.book-created-queue}")
    public void consumeBookCreatedEvent(String message) {
        log.info("Mensagem recebida da fila: {}", message);

        try {
            JsonNode node = objectMapper.readTree(message);

            UUID bookId = UUID.fromString(node.get("bookId").asText());
            String title = node.get("title").asText();
            String author = node.get("author").asText();

            Book book = new Book(bookId, title, author, "0000000000000", 2024);

            attempt++;

            // 💥 FORÇA ERRO nas primeiras tentativas
            if (attempt <= 2) {
                log.warn("Simulando falha no H2 (tentativa {})", attempt);
                throw new RuntimeException("Erro simulado no banco");
            }

            bookRepositoryPort.save(book);

            log.info("Livro salvo com sucesso: {}", bookId);

        } catch (Exception e) {
            log.error("Erro ao processar mensagem", e);
            throw new RuntimeException("Falha no processamento (retry)", e);
        }
    }
}
