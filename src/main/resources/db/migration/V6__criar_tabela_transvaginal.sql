CREATE TABLE IF NOT EXISTS public.transvaginal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- identificação e fila
    cod_consulta VARCHAR(20) NOT NULL UNIQUE,
    cod_usuario VARCHAR(20) NOT NULL,
    numero_prontuario VARCHAR(30),
    posicao INTEGER,
    prioridade VARCHAR(100),
    tipo VARCHAR(100),
    situacao VARCHAR(200),
    perfil VARCHAR(100),
    origem_solicitacao VARCHAR(200),

    -- paciente
    sexo VARCHAR(10),
    nascimento DATE,
    idade INTEGER,
    bairro VARCHAR(120),
    municipio VARCHAR(120),
    municipio_usuario VARCHAR(120),
    microarea VARCHAR(120),
    equipe VARCHAR(120),

    -- clínico
    cid_10 VARCHAR(10),
    descricao_cid VARCHAR(500),
    cid_10_prioritario BOOLEAN,
    qtde_solicitada INTEGER,
    dia_minimo_sessao INTEGER,
    motivo_encaminhamento TEXT,
    procedimento_principal VARCHAR(500),
    procedimento_sms VARCHAR(500),
    procedimentos_sus VARCHAR(500),

    -- sim/não
    regulacao BOOLEAN,
    busca_ativa BOOLEAN,
    processo_automatico BOOLEAN,
    solicitacao_leito BOOLEAN,

    -- datas
    dt_cadastro TIMESTAMP,
    dt_ultima_alteracao TIMESTAMP,
    data_consulta TIMESTAMP,
    data_reservada TIMESTAMP,
    limite_confirmacao_reserva TIMESTAMP,
    dt_realizacao_agendamento TIMESTAMP,
    dt_regulacao TIMESTAMP,
    data_negativa DATE,
    data_controle DATE,

    -- pessoas e unidades
    estabelecimento_solicitante VARCHAR(255),
    estabelecimento_sugerido VARCHAR(255),
    estabelecimento_prestador VARCHAR(255),
    endereco_estabelecimento_prestador VARCHAR(500),
    profissional_sugerido VARCHAR(255),
    profissional_solicitante VARCHAR(255),
    regulador VARCHAR(255),
    cadastrado_por VARCHAR(255),
    nome_responsavel VARCHAR(255),
    nome_responsavel_busca_ativa VARCHAR(255),
    estab_responsavel_busca_ativa VARCHAR(255),

    -- negativa e cancelamento
    motivo_negativa VARCHAR(500),
    justif_negativa TEXT,
    resp_negativa VARCHAR(255),
    justif_cancelamento TEXT,

    -- controle
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transvaginal_cod_usuario ON public.transvaginal (cod_usuario);
CREATE INDEX IF NOT EXISTS idx_transvaginal_situacao ON public.transvaginal (situacao);
CREATE INDEX IF NOT EXISTS idx_transvaginal_prioridade ON public.transvaginal (prioridade);
CREATE INDEX IF NOT EXISTS idx_transvaginal_dt_cadastro ON public.transvaginal (dt_cadastro);