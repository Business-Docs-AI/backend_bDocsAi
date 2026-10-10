-- Conteúdo estruturado (JSON) versionável junto com o HTML — decisão 5/B3: guarda SÓ o
-- conteúdo (objetivo, escopo, fluxo, regras...), que mapeia 1:1 com o conteudo_html
-- renderizado. Metadados de processo/governança ficam SÓ nas colunas da Etapa 2 (nunca aqui).
-- Nullable/aditivo nas 3 tabelas, pra ser versionado (documento_versao) e restaurado junto
-- com o HTML, e pra existir já na proposta (documento_rascunho) antes de confirmada.
ALTER TABLE documento ADD COLUMN conteudo_estruturado JSONB;
ALTER TABLE documento ADD COLUMN versao_schema VARCHAR(20);

ALTER TABLE documento_versao ADD COLUMN conteudo_estruturado JSONB;
ALTER TABLE documento_versao ADD COLUMN versao_schema VARCHAR(20);

ALTER TABLE documento_rascunho ADD COLUMN conteudo_estruturado JSONB;
ALTER TABLE documento_rascunho ADD COLUMN versao_schema VARCHAR(20);
