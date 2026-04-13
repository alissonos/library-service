package com.library.unit;

import com.library.application.service.BookService;
import com.library.domain.model.Book;
import com.library.domain.port.BookEventPublisherPort;
import com.library.domain.port.BookRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  TESTE UNITÁRIO — BookService                                ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • JUnit 5: @Test, @DisplayName, @BeforeEach                ║
 * ║  • Mockito: @Mock (dobros de teste) + @InjectMocks           ║
 * ║  • @ExtendWith(MockitoExtension.class): sem Spring Context   ║
 * ║    → teste rápido, isolado, sem banco ou broker              ║
 * ║  • AssertJ: fluent assertions (melhor que assertEquals)      ║
 * ║  • ArgumentCaptor: verifica o que foi passado ao mock         ║
 * ║                                                              ║
 * ║  PROPÓSITO: testar APENAS a lógica do BookService e do       ║
 * ║  domínio (Book). As dependências são mockadas.               ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@ExtendWith(MockitoExtension.class) // Inicializa mocks sem subir Spring
class BookServiceTest {

    // ── Mocks: substitutos das dependências reais ─────────────────
    @Mock
    private BookRepositoryPort repositoryPort;

    @Mock
    private BookEventPublisherPort eventPublisherPort;

    // ── Subject Under Test: instância real do serviço ─────────────
    @InjectMocks
    private BookService bookService;

    private static final String VALID_ISBN   = "9780132350884"; // 13 dígitos
    private static final String INVALID_ISBN = "123";           // inválido

    // ── Configuração do mock antes de cada teste ──────────────────
    @BeforeEach
    void setUp() {
        // Quando repositoryPort.save() for chamado com qualquer Book,
        // retorna o mesmo Book (simula persistência)
        when(repositoryPort.save(any(Book.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("deve criar livro com dados válidos e publicar evento")
    void shouldCreateBookAndPublishEvent() {
        // ARRANGE — dados de entrada
        String title  = "Clean Code";
        String author = "Robert C. Martin";
        int year      = 2008;

        // ACT — executa o caso de uso
        Book result = bookService.createBook(title, author, VALID_ISBN, year);

        // ASSERT — verifica o resultado
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getAuthor()).isEqualTo(author);
        assertThat(result.getIsbn()).isEqualTo(VALID_ISBN);

        // Verifica que o repositório foi chamado exatamente 1 vez
        verify(repositoryPort, times(1)).save(any(Book.class));

        // Verifica que o evento foi publicado com o livro correto
        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(eventPublisherPort, times(1)).publishBookCreated(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo(title);
    }

    @Test
    @DisplayName("deve lançar exceção quando ISBN é inválido")
    void shouldThrowExceptionWhenIsbnIsInvalid() {
        // ASSERT + ACT — verifica que a exceção é lançada
        assertThatThrownBy(() ->
                bookService.createBook("Título", "Autor", INVALID_ISBN, 2020)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ISBN inválido");

        // Garante que NÃO persistiu nem publicou evento para ISBN inválido
        verify(repositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publishBookCreated(any());
    }

    @Test
    @DisplayName("deve identificar livros clássicos usando Stream API")
    void shouldFilterClassicBooksWithStreamApi() {
        // ARRANGE — livro antigo (clássico) e moderno
        var classicBook = new Book("Dom Quixote", "Cervantes", "9780142437230", 1605);
        var modernBook  = new Book("Clean Code",  "Uncle Bob",  VALID_ISBN,     2008);

        // ACT — filtra usando Stream API (método no BookService)
        List<String> classicTitles = bookService.findClassicBookTitles(
                List.of(classicBook, modernBook)
        );

        // ASSERT — apenas Dom Quixote é clássico (> 50 anos)
        assertThat(classicTitles)
                .hasSize(1)
                .containsExactly("Dom Quixote");
    }

    @Test
    @DisplayName("deve retornar Optional vazio quando livro não existe")
    void shouldReturnEmptyOptionalWhenBookNotFound() {
        // ARRANGE
        var randomId = java.util.UUID.randomUUID();
        when(repositoryPort.findById(randomId)).thenReturn(java.util.Optional.empty());

        // ACT
        var result = bookService.findBookById(randomId);

        // ASSERT
        assertThat(result).isEmpty();
    }
}
