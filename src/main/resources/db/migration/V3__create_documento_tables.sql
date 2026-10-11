-- Tabela vigente (fonte da verdade) e histórico imutável de versões dos documentos.

CREATE TABLE documento (
    id               UUID PRIMARY KEY,
    titulo           VARCHAR(500) NOT NULL,
    conteudo_html    TEXT NOT NULL,
    hash_conteudo    VARCHAR(64) NOT NULL,
    versao_atual     INTEGER NOT NULL,
    status_indexacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    criado_por       VARCHAR(255) NOT NULL,
    criado_em        TIMESTAMP NOT NULL,
    atualizado_por   VARCHAR(255),
    atualizado_em    TIMESTAMP,
    deletado         BOOLEAN NOT NULL DEFAULT FALSE,
    excluido_em      TIMESTAMP
);

CREATE TABLE documento_versao (
    id                   UUID PRIMARY KEY,
    documento_id         UUID NOT NULL,
    numero_versao        INTEGER NOT NULL,
    titulo               VARCHAR(500) NOT NULL,
    conteudo_html        TEXT NOT NULL,
    hash_conteudo        VARCHAR(64) NOT NULL,
    autor                VARCHAR(255) NOT NULL,
    criado_em            TIMESTAMP NOT NULL,
    comentario_alteracao VARCHAR(1000),
    CONSTRAINT fk_documento_versao_documento FOREIGN KEY (documento_id) REFERENCES documento (id),
    CONSTRAINT uk_documento_versao_numero UNIQUE (documento_id, numero_versao)
);

CREATE INDEX idx_documento_versao_documento_id ON documento_versao (documento_id);
