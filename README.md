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
