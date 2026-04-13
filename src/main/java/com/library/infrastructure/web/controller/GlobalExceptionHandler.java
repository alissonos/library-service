package com.library.infrastructure.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Infrastructure/Web — Tratamento Global de Exceções  ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • SOLID / SRP: centraliza tratamento de erros HTTP           ║
 * ║  • Spring Boot 3.x: ProblemDetail (RFC 7807) — padrão        ║
 * ║    moderno para respostas de erro em APIs REST                ║
 * ║  • Java Moderno: Stream API para coletar mensagens de erro    ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Erros de validação Bean Validation (@Valid).
     * Retorna 400 Bad Request com detalhes de cada campo inválido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        // Stream API: coleta todas as mensagens de erro dos campos
        String errors = ex.getBindingResult()
                          .getFieldErrors()
                          .stream()
                          .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                          .collect(Collectors.joining("; "));

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors);
        problem.setTitle("Validation Error");
        return problem;
    }

    /**
     * Erros de regras de negócio lançados pelo domínio.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBusinessErrors(IllegalArgumentException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Business Rule Violation");
        return problem;
    }
}
