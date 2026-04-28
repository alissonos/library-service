package com.library.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Infrastructure — DTOs de Eventos para Mensageria    ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Estrutura assíncrona: eventos como contracts entre        ║
 * ║    Publisher e Consumer (independente de Book)               ║
 * ║  • Imutabilidade: Records Java 16+ para DTOs                 ║
 * ║  • Serialização: @JsonProperty para compatibilidade          ║
 * ║  • Versionamento: cada evento tem versão para evolução       ║
 * ║  • Timestamp: rastreabilidade de quando foi gerado            ║
 * ║                                                              ║
 * ║  Nota: Esses DTOs existem APENAS na camada de infra,        ║
 * ║  nunca devem vazar para o domínio ou aplicação.             ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class BookEventDTOs {

    /**
     * Evento de criação de livro publicado na fila.
     *
     * Estrutura:
     * - eventType: sempre "BOOK_CREATED" (permite extensão futura)
     * - eventVersion: versão do schema do evento
     * - timestamp: quando o evento foi gerado
     * - bookId: UUID do livro
     * - title: título do livro
     * - author: autor do livro
     * - isbn: ISBN do livro
     * - publicationYear: ano de publicação
     */
    public record BookCreatedEvent(
            @JsonProperty("eventType")
            String eventType,

            @JsonProperty("eventVersion")
            String eventVersion,

            @JsonProperty("timestamp")
            Instant timestamp,

            @JsonProperty("bookId")
            UUID bookId,

            @JsonProperty("title")
            String title,

            @JsonProperty("author")
            String author,

            @JsonProperty("isbn")
            String isbn,

            @JsonProperty("publicationYear")
            int publicationYear
    ) {
        /**
         * Factory method para criar o evento a partir de um Book
         */
        public static BookCreatedEvent from(
                UUID bookId,
                String title,
                String author,
                String isbn,
                int publicationYear) {
            return new BookCreatedEvent(
                    "BOOK_CREATED",           // eventType fixo
                    "1.0",                    // versão do schema (permite evolução)
                    Instant.now(),            // timestamp de criação
                    bookId,
                    title,
                    author,
                    isbn,
                    publicationYear
            );
        }
    }

    /**
     * Evento de erro ao processar um evento de criação de livro.
     * Preparação para futuro Dead-Letter Queue (DLQ).
     *
     * Será útil para rastrear mensagens que falharam no processamento
     * mesmo após retries.
     */
    public record BookEventFailedEvent(
            @JsonProperty("eventType")
            String eventType,

            @JsonProperty("originalEventType")
            String originalEventType,

            @JsonProperty("timestamp")
            Instant timestamp,

            @JsonProperty("failedAttempts")
            int failedAttempts,

            @JsonProperty("errorMessage")
            String errorMessage,

            @JsonProperty("bookId")
            UUID bookId,

            @JsonProperty("originalPayload")
            String originalPayload
    ) {
        /**
         * Factory method para criar um evento de falha
         */
        public static BookEventFailedEvent from(
                String originalEventType,
                int attempts,
                String errorMessage,
                UUID bookId,
                String originalPayload) {
            return new BookEventFailedEvent(
                    "BOOK_EVENT_FAILED",      // eventType de falha
                    originalEventType,
                    Instant.now(),
                    attempts,
                    errorMessage,
                    bookId,
                    originalPayload
            );
        }
    }
}
