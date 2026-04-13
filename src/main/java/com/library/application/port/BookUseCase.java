package com.library.application.port;

import com.library.domain.model.Book;
import java.util.Optional;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Application — Port de Entrada (Input Port / Use Case)║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Arquitetura Hexagonal: "Driving Port" — define os casos   ║
 * ║    de uso que o mundo exterior pode acionar                  ║
 * ║  • SOLID / ISP: contrato mínimo para quem aciona o serviço   ║
 * ║  • SOLID / OCP: novos casos de uso → novas interfaces ou     ║
 * ║    novos métodos, sem quebrar implementações existentes       ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public interface BookUseCase {

    /**
     * Caso de uso: Cadastrar um novo livro.
     * Retorna o livro salvo com o ID gerado.
     */
    Book createBook(String title, String author, String isbn, int publicationYear);

    /**
     * Caso de uso: Buscar um livro por ID.
     */
    Optional<Book> findBookById(UUID id);
}
