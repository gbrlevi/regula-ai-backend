-- Contas de acesso à API.
--
-- Nota: a tabela `profissionais` (entidade Usuario) NÃO é uma tabela de login --
-- ela guarda profissionais de saúde vindos da importação do CSV. A tabela
-- `usuarios` do esquema FHIR original foi removida na V4__remodelagem_fila.sql.
--
-- Nenhuma credencial é semeada aqui: a conta inicial é criada em tempo de
-- execução por ContaAcessoBootstrap, a partir de variáveis de ambiente, para
-- que nenhum hash de senha seja versionado no Git.
--
-- IF NOT EXISTS porque o profile `dev` usa ddl-auto: update e, hoje, o .env local
-- aponta para o mesmo banco de produção: se alguém rodar a aplicação local antes
-- do deploy, o Hibernate cria a tabela e esta migration falharia no Flyway.

CREATE TABLE IF NOT EXISTS contas_acesso (
    id            UUID         PRIMARY KEY,
    usuario       VARCHAR(100) NOT NULL UNIQUE,
    senha_hash    VARCHAR(100) NOT NULL,
    papeis        VARCHAR(255) NOT NULL DEFAULT 'ROLE_USUARIO',
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMP    NOT NULL,
    atualizado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contas_acesso_usuario ON contas_acesso (usuario);
