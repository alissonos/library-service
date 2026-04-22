package com.library.application.service;

import com.library.application.port.BookUseCase;
import com.library.domain.model.Book;
import com.library.domain.port.BookEventPublisherPort;
import com.library.domain.port.BookRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  CAMADA: Application — Serviço / Orquestrador de Casos de Uso║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • Arquitetura Hexagonal: implementa o Input Port e           ║
 * ║    delega para os Output Ports (repositório + mensageria)    ║
 * ║  • SOLID / SRP: orquestra o fluxo, não contém regra de       ║
 * ║    negócio (isso fica no domínio) nem detalhe de infra       ║
 * ║  • SOLID / DIP: injeta interfaces, não implementações        ║
 * ║  • SOLID / OCP: para novo caso de uso, adicione método        ║
 * ║    ou nova implementação de BookUseCase sem alterar isto     ║
 * ║  • Java Moderno: Stream API para filtrar livros clássicos    ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@Service // Spring gerencia o ciclo de vida; única dependência de framework aqui
public class BookService implements BookUseCase {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    // ── Injeção por construtor (melhor prática: não usa @Autowired no campo) ──
    // SOLID/DIP: depende de abstrações (interfaces), não de concretos
    private final BookRepositoryPort   repositoryPort;
    private final BookEventPublisherPort eventPublisherPort;

    public BookService(BookRepositoryPort repositoryPort,
                       BookEventPublisherPort eventPublisherPort) {
        this.repositoryPort     = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    // ── Caso de Uso 1: Criar Livro ────────────────────────────────

    @Override
    public Book createBook(String title, String author, String isbn, int publicationYear) {
        log.debug("Iniciando criação de livro: title={}, author={}", title, author);

        // 1. Cria a entidade de domínio (ID gerado internamente no Book)
        var book = new Book(title, author, isbn, publicationYear);

        // 2. Validação de regra de negócio delegada ao domínio
        if (!book.hasValidIsbn()) {
            throw new IllegalArgumentException(
                "ISBN inválido: deve ter exatamente 13 dígitos. Fornecido: " + isbn
            );
        }

        // 3. Persiste via porta de saída (sem saber que é JPA + H2)
        var savedBook = repositoryPort.save(book);
        log.info("Livro salvo: id={}", savedBook.getId());

        // 4. Publica evento via porta de saída (sem saber que é RabbitMQ)
        eventPublisherPort.publishBookCreated(savedBook);

        return savedBook;
    }

    // ── Caso de Uso 2: Buscar Livro por ID ───────────────────────

    @Override
    public Optional<Book> findBookById(UUID id) {
        log.debug("Buscando livro: id={}", id);
        return repositoryPort.findById(id);
    }

    // ── Caso de Uso 3: Atualizar Livro ───────────────────────────────

    @Override
    public Book updateBook(UUID id, String title, String author, String isbn, int publicationYear) {
        log.debug("Atualizando livro: id={}, title={}", id, title);

        var book = repositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado para o ID: " + id));

        book.update(title, author, isbn, publicationYear);

        if (!book.hasValidIsbn()) {
            throw new IllegalArgumentException(
                    "ISBN inválido: deve ter exatamente 13 dígitos. Fornecido: " + isbn
            );
        }

        var savedBook = repositoryPort.save(book);
        log.info("Livro atualizado: id={}", savedBook.getId());

        // Poderia publicar um evento BookUpdatedEvent aqui se necessário
        
        return savedBook;
    }

    // ── Caso de Uso 4: Remover Livro ─────────────────────────────────

    @Override
    public void deleteBook(UUID id) {
        log.debug("Removendo livro: id={}", id);
        // Verifica se existe (opcional, dependendo de como você quer lidar com NotFound)
        repositoryPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado para o ID: " + id));
        
        repositoryPort.deleteById(id);
        log.info("Livro removido: id={}", id);
    }

    // ── Método auxiliar com Stream API ────────────────────────────

    /**
     * Exemplo de uso de Stream API para filtrar livros clássicos.
     * Demonstra Java moderno: filter, map, toList() (Java 16+).
     *
     * Em um projeto real, isso seria exposto como caso de uso separado.
     */
    public List<String> findClassicBookTitles(List<Book> books) {
        return books.stream()                         // Stream<Book>
                    .filter(Book::isClassic)           // filtra clássicos (regra no domínio)
                    .map(Book::getTitle)               // extrai títulos
                    .sorted()                          // ordena alfabeticamente
                    .toList();                         // Java 16+ — lista imutável
    }
}
