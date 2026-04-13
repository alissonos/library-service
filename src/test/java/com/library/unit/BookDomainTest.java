package com.library.unit;

import com.library.domain.model.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  TESTE UNITÁRIO — Entidade Book (Domínio Puro)               ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  Testa as regras de negócio da entidade diretamente.         ║
 * ║  Zero Spring, zero mocks — o teste mais rápido possível.     ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
class BookDomainTest {

    @Test
    @DisplayName("livro publicado há mais de 50 anos deve ser clássico")
    void shouldBeClassicWhenOlderThan50Years() {
        var book = new Book("Hamlet", "Shakespeare", "9780521618748", 1600);
        assertThat(book.isClassic()).isTrue();
    }

    @Test
    @DisplayName("livro recente não deve ser clássico")
    void shouldNotBeClassicWhenRecent() {
        var book = new Book("Clean Code", "Uncle Bob", "9780132350884", 2008);
        assertThat(book.isClassic()).isFalse();
    }

    @Test
    @DisplayName("ISBN com 13 dígitos deve ser válido")
    void shouldHaveValidIsbn() {
        var book = new Book("Test", "Author", "9780132350884", 2020);
        assertThat(book.hasValidIsbn()).isTrue();
    }

    @Test
    @DisplayName("ISBN com menos de 13 dígitos deve ser inválido")
    void shouldHaveInvalidIsbn() {
        var book = new Book("Test", "Author", "123", 2020);
        assertThat(book.hasValidIsbn()).isFalse();
    }

    @Test
    @DisplayName("livro criado sem ID explícito deve receber UUID automaticamente")
    void shouldAutoGenerateId() {
        var book = new Book("Test", "Author", "9780132350884", 2020);
        assertThat(book.getId()).isNotNull();
    }
}
