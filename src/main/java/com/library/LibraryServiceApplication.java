package com.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  Entry Point — library-service                               ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  @SpringBootApplication combina:                             ║
 * ║  • @Configuration — classe de configuração Spring            ║
 * ║  • @EnableAutoConfiguration — configura beans automaticamente ║
 * ║  • @ComponentScan — escaneia pacotes filhos para @Component   ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@SpringBootApplication
public class LibraryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryServiceApplication.class, args);
    }
}
