package com.library.infrastructure.web.controller;

import com.library.application.port.BookUseCase;
import com.library.domain.model.Book;
import com.library.infrastructure.web.dto.BookDTOs.BookResponse;
import com.library.infrastructure.web.dto.BookDTOs.CreateBookRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Infrastructure/Web — Adapter HTTP (Driving Adapter) ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Arquitetura Hexagonal: "Driving Adapter" — converte HTTP  ║
 * ║    em chamada ao caso de uso (Input Port)                    ║
 * ║  • SOLID / SRP: responsável apenas por tratar HTTP           ║
 * ║    (parse request, chamar use case, montar response)         ║
 * ║  • SOLID / DIP: depende de BookUseCase (interface),          ║
 * ║    não de BookService diretamente                            ║
 * ║  • REST semântico: 201 Created com Location header no POST   ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@RestController
@RequestMapping("/books")
public class BookController {

    // Depende da abstração (Input Port), não da implementação concreta
    private final BookUseCase bookUseCase;

    public BookController(BookUseCase bookUseCase) {
        this.bookUseCase = bookUseCase;
    }

    /**
     * POST /books
     * Cadastra um novo livro.
     *
     * Boas práticas REST:
     * — Retorna 201 Created (não 200 OK)
     * — Header Location aponta para o recurso criado
     * — Corpo: representação do recurso criado
     */
    @PostMapping
    public ResponseEntity<BookResponse> createBook(
            @RequestBody @Valid CreateBookRequest request) {

        // 1. Chama o caso de uso (não contém lógica de negócio aqui)
        Book created = bookUseCase.createBook(
                request.title(),
                request.author(),
                request.isbn(),
                request.publicationYear()
        );

        // 2. Constrói o header Location: /books/{id}
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        // 3. Mapeia domínio → DTO de resposta e retorna 201
        return ResponseEntity.created(location).body(toResponse(created));
    }

    /**
     * GET /books/{id}
     * Busca um livro por ID.
     *
     * — 200 OK com corpo quando encontrado
     * — 404 Not Found quando não existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID id) {
        return bookUseCase.findBookById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        // Stream/Optional API: elegante, sem if-else
    }

    // ── Mapeamento DTO ────────────────────────────────────────────

    /**
     * Converte Book (domínio) → BookResponse (DTO de saída).
     * Em projetos maiores: extraia para BookMapper (com ou sem MapStruct).
     */
    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId().toString(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublicationYear(),
                book.isClassic()   // regra de domínio exposta na resposta
        );
    }
}
