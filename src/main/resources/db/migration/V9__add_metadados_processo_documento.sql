-- Metadados de processo/governança para a documentação estruturada (pirâmide ISO 9001).
-- Todas as colunas são nullable e aditivas — nenhum dado existente é alterado além do
-- backfill explícito abaixo. DEFAULT nas duas colunas abaixo cobre inserções brutas (seeds,
-- SQL manual) que não mencionem a coluna; o código Java (DocumentoService.criar) também
-- seta esses dois valores explicitamente, pois o Hibernate envia NULL explícito para colunas
-- não setadas no objeto Java, o que bypassaria o DEFAULT do banco.
ALTER TABLE documento ADD COLUMN tipo_documento VARCHAR(20) DEFAULT 'NAO_CLASSIFICADO';
ALTER TABLE documento ADD COLUMN status_ciclo_vida VARCHAR(20) DEFAULT 'VIGENTE';
ALTER TABLE documento ADD COLUMN dono_processo VARCHAR(255);
ALTER TABLE documento ADD COLUMN aprovador VARCHAR(255);
ALTER TABLE documento ADD COLUMN data_vigencia DATE;
ALTER TABLE documento ADD COLUMN proxima_revisao DATE;
-- Em meses (ex.: 12 = revisão anual) — usado para calcular proxima_revisao.
ALTER TABLE documento ADD COLUMN periodicidade_revisao INTEGER;
ALTER TABLE documento ADD COLUMN confidencialidade VARCHAR(20);
ALTER TABLE documento ADD COLUMN tags TEXT[];

-- Backfill explícito (auditoria) — redundante com o DEFAULT acima para linhas já existentes,
-- mas documentado aqui por clareza.
UPDATE documento SET tipo_documento = 'NAO_CLASSIFICADO' WHERE tipo_documento IS NULL;
UPDATE documento SET status_ciclo_vida = 'VIGENTE' WHERE status_ciclo_vida IS NULL;
