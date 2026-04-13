package com.library.infrastructure.persistence.repository;

import com.library.infrastructure.persistence.entity.BookJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Infrastructure — Spring Data JPA Repository         ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Spring Data JPA: geração automática de CRUD               ║
 * ║  • Arquitetura Hexagonal: concreto de infra — visível apenas  ║
 * ║    dentro de infrastructure.persistence                      ║
 * ║                                                              ║
 * ║  Esta interface NÃO é o BookRepositoryPort. Ela é usada      ║
 * ║  internamente pelo BookRepositoryAdapter para cumprir         ║
 * ║  o contrato do Port de domínio.                              ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public interface SpringBookRepository extends JpaRepository<BookJpaEntity, String> {
    // Spring Data gera: save, findById, findAll, delete...
    // Métodos customizados podem ser adicionados aqui conforme necessidade.
}
