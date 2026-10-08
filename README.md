# regula-ai-backend

API REST do Regula AI, construída com Spring Boot 3.4 e Java 21. Autenticação JWT, rate limiting e PostgreSQL.

## Pré-requisitos

- JDK 21
- Docker (opcional)

## Como rodar

```bash
# 1. Copie o exemplo de variáveis de ambiente e preencha
cp .env.example .env

# 2. Rode em modo dev (perfil dev ativo por padrão)
./gradlew bootRun
```

A aplicação sobe na porta **8080**. Em dev, o Swagger UI fica em `http://localhost:8080/swagger-ui.html`.

> Em dev há valores padrão de conveniência para banco e autenticação; em produção, sem `JWT_SECRET` a aplicação não sobe.

## Comandos principais

| Comando | Descrição |
| --- | --- |
| `./gradlew bootRun` | Roda a aplicação (perfil `dev`) |
| `./gradlew test` | Roda os testes (H2 em memória) |
| `./gradlew build` | Gera o JAR em `build/libs/` |
| `docker compose up --build` | Roda em container (usa `.env.dev`, porta 8080) |

## Popular o banco com um CSV

O `ImportacaoRunner` importa encaminhamentos em blocos de 500 linhas. Para executá-lo no IntelliJ IDEA:

1. Abra o projeto como um projeto Gradle e configure o JDK 21.
2. Inicie o PostgreSQL e configure as credenciais em `.env`, na raiz do repositório.
3. Acesse **Run → Edit Configurations… → + → Application** e configure:
   - **Main class:** `br.com.petsaude.regula_ai_backend.ImportacaoRunner`
   - **Use classpath of module:** selecione o módulo principal do projeto
   - **Program arguments:** caminho do arquivo CSV, por exemplo `dados/encaminhamentos.csv`
   - **Working directory:** raiz do repositório
4. Execute a configuração.

O diretório de trabalho na raiz permite carregar as configurações do perfil `dev` e o arquivo `.env`. O CSV deve estar em UTF-8 e ter um cabeçalho que contenha `Cód Usuário` e `Cod Consulta`; o delimitador é detectado automaticamente. O importador registra no log quantas linhas foram criadas, atualizadas ou tiveram erro.

## Variáveis de ambiente

| Variável | Descrição |
| --- | --- |
| `DB_HOST` | Host do PostgreSQL |
| `DB_PORT` | Porta do PostgreSQL |
| `DB_NAME` | Nome do banco |
| `DB_USER` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Segredo HS256 do JWT (mínimo 32 bytes). Gere com `openssl rand -base64 48`. Obrigatório em produção |
| `AUTH_BOOTSTRAP_USUARIO` | Conta inicial de acesso à API |
| `AUTH_BOOTSTRAP_SENHA` | Senha da conta inicial (persistida apenas como hash BCrypt) |
| `APP_CORS_ORIGENS_PERMITIDAS` | Origens liberadas no CORS, separadas por vírgula, sem barra final (produção) |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo: `dev` (padrão) ou `prod` |

## Infraestrutura

- **PostgreSQL 14+** — em produção o schema é gerenciado pelo Flyway; em dev, pelo Hibernate (`ddl-auto: update`)
- **Porta 8080** — API HTTP, geralmente atrás de um proxy reverso
- **Swagger UI** — apenas em dev (`/swagger-ui.html`); fechado em produção
