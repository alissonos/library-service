package com.library.infrastructure.persistence.adapter;

import com.library.domain.model.Book;
import com.library.domain.port.BookRepositoryPort;
import com.library.infrastructure.persistence.entity.BookJpaEntity;
import com.library.infrastructure.persistence.repository.SpringBookRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Infrastructure — Adapter de Persistência            ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Arquitetura Hexagonal: "Driven Adapter" — implementa o    ║
 * ║    Output Port definido pelo domínio usando JPA              ║
 * ║  • SOLID / DIP: o domínio enxerga BookRepositoryPort;        ║
 * ║    esta classe é o "como" que o domínio não precisa saber    ║
 * ║  • SOLID / SRP: única responsabilidade — traduzir entre      ║
 * ║    Book (domínio) e BookJpaEntity (JPA)                      ║
 * ║                                                              ║
 * ║  Padrão: Anti-corruption Layer entre domínio e JPA.         ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Component
public class BookRepositoryAdapter implements BookRepositoryPort {

    private final SpringBookRepository springBookRepository;

    public BookRepositoryAdapter(SpringBookRepository springBookRepository) {
        this.springBookRepository = springBookRepository;
    }

    @Override
    public Book save(Book book) {
        // 1. Converte entidade de domínio → entidade JPA
        var jpaEntity = toJpaEntity(book);

        // 2. Persiste usando Spring Data
        var saved = springBookRepository.save(jpaEntity);

        // 3. Converte de volta → entidade de domínio
        return toDomain(saved);
    }

    @Override
    public Optional<Book> findById(UUID id) {
        return springBookRepository
                .findById(id.toString())   // UUID → String (chave da tabela)
                .map(this::toDomain);      // Stream API + method reference
    }

    // ── Mapeamento manual (substitui MapStruct para simplicidade) ──

    /**
     * Domínio → JPA: extrai dados primitivos do Book e cria BookJpaEntity.
     * MapStruct geraria este código automaticamente via @Mapper.
     */
    private BookJpaEntity toJpaEntity(Book book) {
        return new BookJpaEntity(
                book.getId().toString(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublicationYear()
        );
    }

    /**
     * JPA → Domínio: reconstrói o objeto de domínio a partir dos dados do banco.
     */
    private Book toDomain(BookJpaEntity entity) {
        return new Book(
                UUID.fromString(entity.getId()),
                entity.getTitle(),
                entity.getAuthor(),
                entity.getIsbn(),
                entity.getPublicationYear()
        );
    }
}
