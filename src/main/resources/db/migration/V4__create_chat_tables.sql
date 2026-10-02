-- Substitui a tabela "chat" (stub sem dono/papéis, nunca usada de verdade) pelo histórico
-- de conversas de fato: cada conversa pertence a um usuário; cada mensagem tem papel,
-- conteúdo e as fontes usadas na resposta (quando aplicável).

DROP TABLE IF EXISTS chat;

CREATE TABLE chat_conversa (
    id         UUID PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    titulo     VARCHAR(255),
    criado_em  TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_conversa_usuario FOREIGN KEY (usuario_id) REFERENCES user_entity (id)
);

CREATE INDEX idx_chat_conversa_usuario_id ON chat_conversa (usuario_id);

CREATE TABLE chat_mensagem (
    id          UUID PRIMARY KEY,
    conversa_id UUID NOT NULL,
    papel       VARCHAR(20) NOT NULL,
    conteudo    TEXT NOT NULL,
    fontes      TEXT,
    criado_em   TIMESTAMP NOT NULL,
    CONSTRAINT fk_chat_mensagem_conversa FOREIGN KEY (conversa_id) REFERENCES chat_conversa (id)
);

CREATE INDEX idx_chat_mensagem_conversa_id ON chat_mensagem (conversa_id);
