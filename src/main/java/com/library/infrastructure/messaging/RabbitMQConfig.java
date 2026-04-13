package com.library.infrastructure.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Infrastructure — Configuração do RabbitMQ           ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Infraestrutura como código: fila declarada via @Bean       ║
 * ║  • SOLID / SRP: classe responsável apenas pela config do MQ  ║
 * ║  • Separação de concerns: config fora do adapter             ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.messaging.book-created-queue}")
    private String bookCreatedQueue;

    /**
     * Declara a fila no broker.
     * durable=true → fila sobrevive a restart do RabbitMQ.
     * Se a fila já existir, o broker a reutiliza sem erro.
     */
    @Bean
    public Queue bookCreatedQueue() {
        return new Queue(bookCreatedQueue, /* durable= */ true);
    }
}
