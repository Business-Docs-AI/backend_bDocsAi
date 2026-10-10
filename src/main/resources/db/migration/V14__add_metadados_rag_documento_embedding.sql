-- Etapa 14: novos metadados do chunk do RAG (filtro pré-busca, Etapa 16). Aditivo e
-- nullable — chunks já indexados (legados) ficam com essas colunas NULL; a Etapa 16 trata
-- NULL em status_ciclo_vida como VIGENTE (nenhum documento existente pode sumir do RAG).
--
-- B1: ALTER TABLE ... ADD COLUMN IF NOT EXISTS cobre o banco EXISTENTE (a tabela já existe,
-- criada por PgVectorEmbeddingStore.createTable(true) numa versão anterior do
-- columnDefinitions). Um banco NOVO (sem a tabela ainda) é coberto pelo columnDefinitions
-- atualizado em EmbeddingConfig — createTable(true) já cria a tabela com as colunas novas
-- de uma vez, tornando este ALTER um no-op (IF NOT EXISTS) nesse caso.
ALTER TABLE IF EXISTS documento_embedding
    ADD COLUMN IF NOT EXISTS categoria_id BIGINT,
    ADD COLUMN IF NOT EXISTS tipo_documento TEXT,
    ADD COLUMN IF NOT EXISTS status_ciclo_vida TEXT,
    ADD COLUMN IF NOT EXISTS macroprocesso_id BIGINT,
    ADD COLUMN IF NOT EXISTS confidencialidade TEXT;
