-- Hierarquia de processo (Macroprocesso → Processo → Subprocesso → Atividade), complementar
-- às categorias — o setor (categoria/área dona) continua sendo o único controle de acesso.
CREATE TABLE macroprocesso (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descricao VARCHAR(1000)
);

ALTER TABLE documento ADD COLUMN macroprocesso_id BIGINT;
ALTER TABLE documento ADD COLUMN processo_pai_id UUID;

ALTER TABLE documento
    ADD CONSTRAINT fk_documento_macroprocesso
    FOREIGN KEY (macroprocesso_id) REFERENCES macroprocesso (id);

-- ON DELETE SET NULL: excluir (soft delete não passa por aqui, é hard delete de um
-- macroprocesso/documento que nunca existiu de fato) o pai nunca derruba o filho.
ALTER TABLE documento
    ADD CONSTRAINT fk_documento_processo_pai
    FOREIGN KEY (processo_pai_id) REFERENCES documento (id) ON DELETE SET NULL;
