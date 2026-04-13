package com.library.domain.model;

import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Domain — Entidade de Negócio                        ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Arquitetura Hexagonal: núcleo puro, zero imports Spring   ║
 * ║  • SOLID / SRP: representa apenas "o que é um Livro"         ║
 * ║  • SOLID / DIP: não conhece JPA, HTTP ou qualquer framework  ║
 * ║                                                              ║
 * ║  Esta classe é a "verdade" do negócio. Pode ser testada      ║
 * ║  sem subir nenhum contexto Spring.                           ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class Book {

    private UUID id;
    private String title;
    private String author;
    private String isbn;
    private int publicationYear;

    // Construtor sem ID — usado antes de persistir (ID gerado no domínio)
    public Book(String title, String author, String isbn, int publicationYear) {
        this.id = UUID.randomUUID(); // Regra: ID gerado pelo domínio, não pelo banco
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publicationYear = publicationYear;
    }

    // Construtor completo — usado ao reconstituir do banco
    public Book(UUID id, String title, String author, String isbn, int publicationYear) {
        this.id    = id;
        this.title = title;
        this.author = author;
        this.isbn   = isbn;
        this.publicationYear = publicationYear;
    }

    // ── Regras de negócio do domínio ──────────────────────────────

    /**
     * Verifica se o livro é considerado "clássico" (publicado há mais de 50 anos).
     * Regra de negócio pura — testável sem Spring.
     * Demonstra: lógica no domínio, não no serviço ou controller.
     */
    public boolean isClassic() {
        int currentYear = java.time.Year.now().getValue();
        return (currentYear - this.publicationYear) > 50;
    }

    /**
     * Valida se o ISBN tem exatamente 13 dígitos numéricos.
     * Outra regra que pertence à entidade, não à camada de aplicação.
     */
    public boolean hasValidIsbn() {
        return isbn != null && isbn.matches("\\d{13}");
    }

    // ── Getters (sem setters para manter imutabilidade parcial) ───

    public UUID getId()              { return id; }
    public String getTitle()         { return title; }
    public String getAuthor()        { return author; }
    public String getIsbn()          { return isbn; }
    public int getPublicationYear()  { return publicationYear; }

    @Override
    public String toString() {
        return "Book{id=" + id + ", title='" + title + "', author='" + author + "'}";
    }
}
