-- Rascunhos de criação/atualização propostos pela IA no chat. Nunca são apagados (servem de
-- histórico/auditoria, inclusive de propostas nunca confirmadas). Só viram um "documento" de
-- verdade (e só aí geram embeddings) quando confirmados — e a confirmação só é aceita se o
-- rascunho foi proposto em um turno de conversa anterior ao turno da confirmação.

CREATE TABLE documento_rascunho (
    id                UUID PRIMARY KEY,
    conversa_id       UUID NOT NULL,
    tipo              VARCHAR(20) NOT NULL,
    documento_id_alvo UUID,
    titulo            VARCHAR(500) NOT NULL,
    conteudo_html     TEXT NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    turno_criacao     UUID NOT NULL,
    criado_em         TIMESTAMP NOT NULL,
    confirmado_em     TIMESTAMP,
    CONSTRAINT fk_documento_rascunho_conversa FOREIGN KEY (conversa_id) REFERENCES chat_conversa (id)
);

CREATE INDEX idx_documento_rascunho_conversa_status ON documento_rascunho (conversa_id, status);
