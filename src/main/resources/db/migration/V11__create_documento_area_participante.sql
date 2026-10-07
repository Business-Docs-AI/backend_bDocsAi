-- Áreas participantes de um documento — APENAS INFORMATIVO (decisão 3): nunca concede
-- acesso em hipótese alguma. categoria_id (área dona) continua sendo o único controle de
-- acesso, via usuario_categoria/CategoriaAccessService — esta tabela nem é referenciada lá.
CREATE TABLE documento_area_participante (
    id BIGSERIAL PRIMARY KEY,
    documento_id UUID NOT NULL,
    categoria_id BIGINT NOT NULL,
    CONSTRAINT fk_area_participante_documento
        FOREIGN KEY (documento_id) REFERENCES documento (id) ON DELETE CASCADE,
    CONSTRAINT fk_area_participante_categoria
        FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT uq_area_participante UNIQUE (documento_id, categoria_id)
);
