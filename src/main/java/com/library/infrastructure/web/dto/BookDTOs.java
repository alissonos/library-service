package com.library.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Infrastructure/Web — DTOs como Java Records         ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Java Moderno (Java 16+): Records — imutáveis, concisos    ║
 * ║    O compilador gera: construtor, getters, equals,           ║
 * ║    hashCode e toString automaticamente                       ║
 * ║  • SOLID / SRP: cada record tem um único papel (entrada       ║
 * ║    ou saída da API)                                          ║
 * ║  • Bean Validation (Jakarta): @NotBlank, @Size, @Positive    ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class BookDTOs {

    /**
     * DTO de entrada — corpo do POST /books.
     *
     * Record: imutável por padrão. Perfeito para DTOs de request
     * que não devem ser modificados após a deserialização.
     */
    public record CreateBookRequest(

            @NotBlank(message = "Título é obrigatório")
            String title,

            @NotBlank(message = "Autor é obrigatório")
            String author,

            @NotBlank(message = "ISBN é obrigatório")
            @Size(min = 13, max = 13, message = "ISBN deve ter exatamente 13 caracteres")
            String isbn,

            @Positive(message = "Ano de publicação deve ser positivo")
            int publicationYear

    ) {}

    /**
     * DTO de entrada — corpo do PUT /books/{id}.
     * Semelhante ao de criação, mas semanticamente diferente.
     */
    public record UpdateBookRequest(

            @NotBlank(message = "Título é obrigatório")
            String title,

            @NotBlank(message = "Autor é obrigatório")
            String author,

            @NotBlank(message = "ISBN é obrigatório")
            @Size(min = 13, max = 13, message = "ISBN deve ter exatamente 13 caracteres")
            String isbn,

            @Positive(message = "Ano de publicação deve ser positivo")
            int publicationYear

    ) {}

    /**
     * DTO de saída — retornado nas respostas GET e POST.
     *
     * Inclui 'isClassic' — calculado no domínio, exposto na API.
     * O controller ou mapper decide o que incluir na resposta.
     */
    public record BookResponse(
            String id,
            String title,
            String author,
            String isbn,
            int publicationYear,
            boolean isClassic
    ) {}
}
