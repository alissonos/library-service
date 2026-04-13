package com.library.domain.port;

import com.library.domain.model.Book;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Domain — Port de Saída para Mensageria              ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Arquitetura Hexagonal: Port de saída para o "mundo"       ║
 * ║    de mensageria (driven adapter)                            ║
 * ║  • SOLID / DIP: domínio define o contrato; a infra cumpre    ║
 * ║  • SOLID / ISP: separado de BookRepositoryPort               ║
 * ║                                                              ║
 * ║  O domínio não sabe que existe RabbitMQ.                     ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public interface BookEventPublisherPort {

    /**
     * Publica um evento informando que um livro foi criado.
     * A implementação concreta decide o broker e o formato da mensagem.
     */
    void publishBookCreated(Book book);
}
