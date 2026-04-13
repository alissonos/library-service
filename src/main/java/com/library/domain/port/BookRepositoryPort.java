package com.library.domain.port;

import com.library.domain.model.Book;
import java.util.Optional;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Domain — Port de Saída (Output Port)                ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Arquitetura Hexagonal: "Port" — contrato que o domínio    ║
 * ║    exige da infraestrutura (driven side)                     ║
 * ║  • SOLID / DIP: o domínio depende de abstração (interface),  ║
 * ║    não de implementação concreta (JpaRepository)             ║
 * ║  • SOLID / ISP: interface pequena, focada em persistência    ║
 * ║                                                              ║
 * ║  O domínio "grita" o que precisa — a infra implementa.       ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public interface BookRepositoryPort {

    /**
     * Persiste um livro e retorna a instância salva.
     * O domínio não sabe se isso vai para H2, PostgreSQL ou MongoDB.
     */
    Book save(Book book);

    /**
     * Busca um livro pelo seu identificador único.
     * Optional é Java moderno — evita null e força tratamento explícito.
     */
    Optional<Book> findById(UUID id);
}
