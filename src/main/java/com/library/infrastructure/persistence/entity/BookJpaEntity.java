package com.library.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║ CAMADA: Infrastructure — Entidade JPA ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║ CONCEITOS APLICADOS: ║
 * ║ • Arquitetura Hexagonal: adapter de persistência — detalhe ║
 * ║ de infraestrutura que o domínio desconhece ║
 * ║ • SOLID / SRP: único papel — mapear Book para tabela SQL ║
 * ║ ║
 * ║ Esta classe NÃO circula fora da camada infrastructure. ║
 * ║ O domínio usa Book.java, nunca BookJpaEntity.java. ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(name = "books")
public class BookJpaEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)") // UUID como String no H2
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(name = "publication_year", nullable = false)
    private int publicationYear;

    // JPA exige construtor padrão
    protected BookJpaEntity() {
    }

    public BookJpaEntity(String id, String title, String author,
            String isbn, int publicationYear) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getPublicationYear() {
        return publicationYear;
    }
}
