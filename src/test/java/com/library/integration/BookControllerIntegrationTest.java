package com.library.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.domain.port.BookEventPublisherPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  TESTE DE INTEGRAÇÃO — BookController + BookService + H2     ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  CONCEITOS APLICADOS:                                        ║
 * ║  • @SpringBootTest: sobe contexto Spring completo            ║
 * ║  • @AutoConfigureMockMvc: injeta MockMvc sem servidor real    ║
 * ║  • @MockBean: substitui RabbitMQ por mock (infra externa)    ║
 * ║  • @Transactional: reverte banco após cada teste             ║
 * ║  • @ActiveProfiles("test"): permite config separada          ║
 * ║                                                              ║
 * ║  Diferença dos unitários:                                    ║
 * ║  Aqui testamos o fluxo HTTP → Controller → Service → JPA/H2 ║
 * ║  (toda a pilha, exceto o broker externo)                     ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional          // garante isolamento: banco limpo a cada @Test
@ActiveProfiles("test")
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── @MockBean: substitui o bean real no contexto Spring ────────
    // Evita precisar de um RabbitMQ rodando durante o teste de integração
    @MockBean
    private BookEventPublisherPort eventPublisherPort;

    // ── Constantes de teste ────────────────────────────────────────
    private static final String VALID_ISBN = "9780132350884";

    // ══════════════════════════════════════════════════════════════
    //  Testes: POST /books
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /books — deve criar livro e retornar 201 com Location")
    void shouldCreateBookAndReturn201() throws Exception {
        var body = Map.of(
                "title",           "Clean Code",
                "author",          "Robert C. Martin",
                "isbn",            VALID_ISBN,
                "publicationYear", 2008
        );

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
               // ── Verificações HTTP ──────────────────────────────
               .andExpect(status().isCreated())                    // 201
               .andExpect(header().exists("Location"))             // header Location presente
               .andExpect(jsonPath("$.id").isNotEmpty())           // tem ID gerado
               .andExpect(jsonPath("$.title").value("Clean Code"))
               .andExpect(jsonPath("$.author").value("Robert C. Martin"))
               .andExpect(jsonPath("$.isbn").value(VALID_ISBN))
               .andExpect(jsonPath("$.isClassic").value(false));   // 2008 não é clássico

        // Verifica que o evento de mensageria foi publicado 1 vez
        verify(eventPublisherPort, times(1)).publishBookCreated(any());
    }

    @Test
    @DisplayName("POST /books — deve retornar 400 quando título está em branco")
    void shouldReturn400WhenTitleIsBlank() throws Exception {
        var body = Map.of(
                "title",           "",   // inválido: @NotBlank
                "author",          "Autor",
                "isbn",            VALID_ISBN,
                "publicationYear", 2020
        );

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isBadRequest())                 // 400
               .andExpect(jsonPath("$.detail").value(containsString("title")));

        // Nenhum evento deve ser publicado quando a request é inválida
        verify(eventPublisherPort, never()).publishBookCreated(any());
    }

    @Test
    @DisplayName("POST /books — deve retornar 422 quando ISBN é inválido (regra de domínio)")
    void shouldReturn422WhenIsbnIsInvalid() throws Exception {
        var body = Map.of(
                "title",           "Livro Teste",
                "author",          "Autor",
                "isbn",            "1234567890123",  // 13 chars mas não apenas dígitos válidos?
                "publicationYear", 2020
        );

        // Nota: ISBN "1234567890123" tem 13 dígitos, então passará pelo @Size.
        // A validação de "apenas dígitos" é feita no domínio.
        // Este teste valida o fluxo de ISBN realmente inválido (ex: letras)
        var bodyInvalidChars = Map.of(
                "title",           "Livro Teste",
                "author",          "Autor",
                "isbn",            "ISBN123456789", // contém letras — inválido
                "publicationYear", 2020
        );

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyInvalidChars)))
               .andExpect(status().isUnprocessableEntity());       // 422 (regra de negócio)
    }

    // ══════════════════════════════════════════════════════════════
    //  Testes: GET /books/{id}
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /books/{id} — deve retornar livro existente com 200")
    void shouldReturnBookWhenExists() throws Exception {
        // 1. Cria o livro via POST (mesmo contexto transacional)
        var body = Map.of(
                "title",           "The Pragmatic Programmer",
                "author",          "David Thomas",
                "isbn",            "9780135957059",
                "publicationYear", 1999
        );

        var postResult = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
               .andExpect(status().isCreated())
               .andReturn();

        // 2. Extrai o ID do corpo da resposta
        var responseBody = objectMapper.readTree(
                postResult.getResponse().getContentAsString()
        );
        String bookId = responseBody.get("id").asText();

        // 3. Busca pelo ID via GET
        mockMvc.perform(get("/books/{id}", bookId))
               .andExpect(status().isOk())                         // 200
               .andExpect(jsonPath("$.id").value(bookId))
               .andExpect(jsonPath("$.title").value("The Pragmatic Programmer"))
               .andExpect(jsonPath("$.isClassic").value(false));   // 1999 < 50 anos
    }

    @Test
    @DisplayName("GET /books/{id} — deve retornar 404 quando livro não existe")
    void shouldReturn404WhenBookNotFound() throws Exception {
        String randomId = java.util.UUID.randomUUID().toString();

        mockMvc.perform(get("/books/{id}", randomId))
               .andExpect(status().isNotFound());                  // 404
    }

    @Test
    @DisplayName("GET /books/{id} — livro de 1970 deve ser marcado como clássico")
    void shouldMarkOldBookAsClassic() throws Exception {
        var body = Map.of(
                "title",           "The C Programming Language",
                "author",          "Dennis Ritchie",
                "isbn",            "9780131103627",
                "publicationYear", 1972   // > 50 anos → clássico
        );

        var postResult = mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
               .andReturn();

        String bookId = objectMapper.readTree(
                postResult.getResponse().getContentAsString()
        ).get("id").asText();

        mockMvc.perform(get("/books/{id}", bookId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.isClassic").value(true));    // regra de domínio!
    }
}
