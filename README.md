# 📚 Library Service

## 📖 Descrição
O **Library Service** é um microsserviço focado na gestão de livros e biblioteca. Ele permite o cadastro de novos livros e a consulta de livros existentes, garantindo o processo através de regras bem definidas de domínio.

## 🛠️ Tecnologias
O projeto foi desenvolvido empregando um ecossistema robusto e moderno do mundo Java:
- **Java 17+**
- **Spring Boot (3.2.x)**
- **Spring Data JPA / Hibernate** (Persistência)
- **H2 Database** (Banco de dados em memória para dev/testes)
- **RabbitMQ** (Serviço de mensageria assíncrona / AMQP)
- **Maven** (Gerenciador de dependências)

## 🏗️ Arquitetura
O sistema foi concebido inspirando-se fortemente na **Arquitetura Hexagonal (Ports and Adapters) / Clean Architecture**, visando um grande foco no isolamento das regras de negócio. Sendo assim, a estrutura visual de pacotes reflete essas camadas:

- `domain`: O coração da aplicação. Contém nosso modelo de domínio principal (`Book`) e as portas de saída (`BookRepositoryPort`, `BookEventPublisherPort`), definindo os contratos que a infraestrutura deve implementar.
- `application`: Camada de orquestração responsável pelos Casos de Uso (ex: `BookService` implementando a porta de entrada `BookUseCase`). Não sabe se estamos num ambiente Web ou CLI.
- `infrastructure`: Camada mais externa onde a "sujeira" do mundo real reside (os adaptadores). Compreende as entidades e repositórios JPA (`SpringBookRepository`), Web Controllers (`BookController`), DTOs e as publicações p/ RabbitMQ.

## 🚀 Endpoints

Abaixo detalhamos os endpoints REST expostos pela aplicação (recursos de `/books`):

| Método | Rota | Descrição |
| ------ | ---- | --------- |
| **POST** | `/books` | Cadastra um novo livro na base. Retorna `201 Created` e um header `Location` com a URI de consulta. |
| **GET**  | `/books/{id}` | Busca os detalhes de um livro recém-criado através de seu UUID. Retorna `200 OK` se encontrado ou `404 Not Found`. |
| **DELETE**  | `/books/{id}` | Remove um livro do sistema através de seu UUID. `Retorna 204` No Content em caso de sucesso ou `404 Not Found` |
| **UPDATE**  | `/books/{id}` | Busca os detalhes de um livro recém-criado através de seu UUID. Retorna `200 OK` se encontrado ou `404 Not Found`. |

## ▶️ Como Executar

Para iniciar o projeto na sua máquina local de maneira bem rápida e sem burocracias, utilize o maven wrapper na raiz do repositório:

```bash
./mvnw spring-boot:run
```
*(Observação: Você também pode usar tranquilamente `mvn spring-boot:run` se já tiver o Maven instalado nas suas variáveis de ambiente).*

De acordo com a configuração atual, o servidor estará operante na porta **8081**.

## ⚙️ Configurações

Para personalizar comportamentos do sistema — como as credenciais ou as string loaders do banco de dados — existe o arquivo central de configuração: `/src/main/resources/application.properties`.

**Pontos importantes de configuração:**
* **Banco de dados (H2)**: O banco está operando totalmente em memória com reset no encerramento (DDL `create-drop`). Se você desejar trocar futuramente para, por exemplo, um **PostgreSQL**, o driver e URL devem ser atualizados nesse arquivo. Acompanha também um console do H2 na rota `/h2-console`.
* **RabbitMQ**: O projeto já aponta e espera um ambiente de fila local (em `localhost:5672`). Em um cenário produtivo, modifique aqui o Host/Username/Password.
