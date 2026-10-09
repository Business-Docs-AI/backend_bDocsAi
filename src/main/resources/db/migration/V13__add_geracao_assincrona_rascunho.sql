-- Geração assíncrona do documento estruturado (Etapa 13.1): o rascunho passa a poder
-- existir em GERANDO (worker ainda processando) e ERRO_GERACAO (esgotou tentativas ou
-- falhou) antes de chegar a PENDENTE — os 2 valores novos de status vivem só na coluna
-- STRING existente (StatusRascunho é @Enumerated(STRING), sem enum nativo no banco), não
-- exigem migração de tipo. PENDENTE/CONFIRMADO/DESCARTADO continuam com o mesmo significado
-- de sempre; o fluxo legado (HTML síncrono) nunca entra nos 2 estados novos.

-- Nullable: só o caminho novo deixa null temporariamente enquanto status=GERANDO. O fluxo
-- legado (e o @NotBlank da camada de aplicação nesse caminho) continua sempre preenchendo —
-- nenhuma mudança de comportamento ali. A CHECK constraint abaixo garante, a nível de banco,
-- que PENDENTE/CONFIRMADO nunca ficam com conteudo_html nulo (R6).
ALTER TABLE documento_rascunho ALTER COLUMN conteudo_html DROP NOT NULL;

-- Mensagem de erro clara quando a geração falha (tentativas esgotadas, exceção da API,
-- LENGTH sem chamada de ferramenta utilizável etc.) — nunca usada pelo fluxo legado.
ALTER TABLE documento_rascunho ADD COLUMN erro_geracao TEXT;

-- Contador de tentativas de geração (R3) — começa em 0, incrementado a cada tentativa real
-- do worker (reserva via UPDATE condicional, ver IRascunhoDocumentoRepository). Default 0
-- também protege o fluxo legado, que nunca toca esta coluna.
ALTER TABLE documento_rascunho ADD COLUMN tentativas_geracao INT NOT NULL DEFAULT 0;

-- Guia opcional que o usuário/IA dá na tool leve de solicitação, para focar o worker no
-- processo certo quando a conversa tiver outros assuntos (R4) — nunca usada pelo fluxo legado.
ALTER TABLE documento_rascunho ADD COLUMN instrucoes_adicionais TEXT;

-- Marca quando um worker "reservou" este rascunho para processar (R3) — usada pelo UPDATE
-- condicional que impede o listener e o job de segurança processarem o mesmo rascunho duas
-- vezes, e pelo job de segurança para achar rascunhos presos (reservado há muito tempo sem
-- terminar). Nunca usada pelo fluxo legado.
ALTER TABLE documento_rascunho ADD COLUMN reservado_em TIMESTAMP;

-- R6: trava a nível de banco — nenhum rascunho em PENDENTE ou CONFIRMADO pode ficar sem
-- conteudo_html, mesmo que algum código futuro esqueça de preencher. GERANDO/ERRO_GERACAO/
-- DESCARTADO não são restringidos (podem ou não ter conteudo_html).
ALTER TABLE documento_rascunho ADD CONSTRAINT chk_rascunho_html_obrigatorio_se_pendente_ou_confirmado
    CHECK (status NOT IN ('PENDENTE', 'CONFIRMADO') OR conteudo_html IS NOT NULL);
