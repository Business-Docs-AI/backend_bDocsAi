# Plano — Documentação Estruturada (pirâmide ISO 9001 / SIPOC / RACI / APQC)

Branch: `feature/documentacao-estruturada`. Plano aprovado em 2026-10-05.
Este arquivo é a fonte de verdade do progresso — se a sessão for reiniciada,
retome a partir daqui (coluna "Status").

## Regras invioláveis (resumo)

- Tudo aditivo: nenhuma tabela/coluna/endpoint/contrato existente é removido,
  renomeado ou tem o tipo alterado. Migrations nunca apagam dado.
- Documentos e categorias existentes continuam funcionando exatamente como
  hoje, em todos os fluxos (chat, REST, RAG, versionamento).
- Categoria continua sendo o único controle de acesso — a nova taxonomia
  (macroprocesso, tipo, status de ciclo de vida, etc.) é só metadado.
- Nenhum prompt existente é sobrescrito — sempre uma versão nova (v2) atrás
  de flag, com o v1 intacto e testado por igualdade de string.
- Teste existente nunca pode regredir. Toda etapa fecha com suíte 100% verde.
- Um commit por etapa, só depois do OK explícito do usuário.

## Flags

| Flag | Default | Controla |
|---|---|---|
| `bdocs.documentacao-estruturada.enabled` | `false` | Etapas 1–13, 17 (parte do cálculo de `proxima_revisao`), 18–20 |
| `bdocs.rag.filtros-ciclo-vida.enabled` | `false` | Só a Etapa 16 (leitura/filtro dos metadados na busca). A gravação dos metadados no chunk (Etapa 14) e os gatilhos de reindexação por metadado (Etapa 17) ficam **sempre ligados** (aditivo, decisão B4). |

Propriedade adicional: `bdocs.documentacao-estruturada.provedores` (lista,
ex. `gemini,anthropic`) — define em quais provedores de chat a tool
estruturada fica disponível (decisão A3).

**Nota de operação (B4)**: antes de ligar `bdocs.rag.filtros-ciclo-vida.enabled`
em qualquer ambiente, rodar a reindexação em massa (Etapa 15) primeiro.

## Índice das etapas

| # | Etapa | Status |
|---|---|---|
| 0 | CI (workflow Postgres+pgvector) | 🔄 em andamento |
| 1 | Feature flag `documentacao-estruturada` | ⏳ pendente |
| 2 | Metadados escalares em `documento` | ⏳ pendente |
| 3 | Hierarquia de processo (`macroprocesso` + `processo_pai_id`) | ⏳ pendente |
| 4 | Macroprocesso — tool de listagem + CRUD ADMIN | ⏳ pendente |
| 5 | Endpoint ADMIN de mudança de `status_ciclo_vida` | ⏳ pendente |
| 6 | Áreas participantes (join table informativa) | ⏳ pendente |
| 7 | Conteúdo estruturado versionável (jsonb) | ⏳ pendente |
| 8 | DTOs estruturados + validação (Bean + semântica) | ⏳ pendente |
| 9 | Renderizador HTML determinístico | ⏳ pendente |
| 10 | Investigação: function calling com POJO aninhado | ⏳ pendente |
| 11 | Investigação: orçamento de tokens | ⏳ pendente |
| 12 | Prompt v2 (composto sobre o v1) | ⏳ pendente |
| 13 | Tool estruturada (propor/confirmar/editar) | ⏳ pendente |
| 14 | Metadados novos no chunk do RAG | ⏳ pendente |
| 15 | Reindexação em massa (ADMIN, manual, assíncrona) | ⏳ pendente |
| 16 | Filtro pré-busca no RAG | ⏳ pendente |
| 17 | Reindexação automática em mudança de metadado | ⏳ pendente |
| 18 | Governança (backend) | ⏳ pendente |
| 19 | Governança (frontend) + telas de macroprocesso/status/metadados | ⏳ pendente |
| 20 | Mermaid (última etapa) | ⏳ pendente |

## Decisões de bloqueio e ajuste (aprovadas, 2026-10-05)

- **B1**: `ALTER TABLE IF EXISTS documento_embedding ADD COLUMN IF NOT EXISTS ...`
  (banco existente) + `columnDefinitions` atualizado no `EmbeddingConfig`
  (banco novo via `createTable`). Testar os dois cenários na Etapa 14.
- **B2**: tool `obterDocumentoEstruturado(documentoId)` (flag + acesso por
  categoria); prompt v2 obriga edição via ela + `prepararAtualizacaoDocumentoEstruturado`;
  regra C3 (zerar JSON em update legado) cobre também `confirmarRascunhoPendente`
  de um rascunho ATUALIZAR sem estruturado.
- **B3**: `conteudo_estruturado` em `documento`/`documento_versao` guarda só
  conteúdo (1:1 com o HTML); metadados só nas colunas. No `documento_rascunho`
  o JSON pode vir completo (proposta); a separação ocorre na confirmação.
- **B4**: gatilhos de reindexação por metadado sempre ligados (sem flag de
  RAG); só a leitura dos metadados (Etapa 16) fica atrás de
  `filtros-ciclo-vida`; `proxima_revisao` segue a flag `documentacao-estruturada`.
- **B5**: verificar suporte a `IS NULL` no `Filter` do langchain4j 1.18.0 no
  início da Etapa 16, antes de codar; se não suportar, reportar alternativas
  em vez de improvisar.
- **B6**: nenhum filtro de confidencialidade nesta entrega — só o metadado
  gravado.
- **A1**: `IndexacaoService.gerarSegmentos` já prefixa cada segmento com
  título+seção hoje; complementar com o tipo na Etapa 14.
- **A2**: erro de validação (Bean + semântica) na tool estruturada devolve
  ao LLM uma mensagem objetiva por erro + limite de tentativas por turno.
- **A3**: provedores habilitados via propriedade
  `bdocs.documentacao-estruturada.provedores` (lista), não fixo no código.
- **A4**: DELETE de macroprocesso em uso retorna 409 (FK `RESTRICT`).
- **A5**: frontend (Etapa 19) ganha tela ADMIN de macroprocessos (padrão da
  tela de Categorias), controle ADMIN de status do documento, e metadados
  visíveis no painel do documento.
- **A6**: documento `OBSOLETO` nunca conta como "revisão vencida".
- **A7**: se gemini ou anthropic falharem com o schema da Etapa 8 na
  investigação da Etapa 10, parar e reportar antes de seguir — não
  simplificar o DTO sem consultar o usuário.
- **C1**: ordem do RAG trocada para metadados (14) → reindexação em massa
  (15) → filtro (16), para nunca excluir chunk legado sem metadado do
  acervo durante a transição.
- **C4**: a mesma regra "vigente por padrão, sem metadado = vigente" vale
  também em `PesquisaService` (endpoint `/documentos/busca`), não só no
  RAG do chat.
- **C5**: segunda flag dedicada ao RAG (ver tabela de flags acima).

## Explicitamente adiado (fora do escopo desta entrega)

- Glossário corporativo + expansão de consulta no RAG.
- Templates estruturados de POLITICA / INSTRUCAO / REGISTRO (esta entrega
  cobre só PROCESSO e PROCEDIMENTO).
- Fluxo de revisão/aprovação (EM_ELABORACAO → EM_REVISAO → VIGENTE).
- Regras de acesso por confidencialidade.
- Limpeza do código legado (`conversation`, `uploadFiles`, `permission_id`).

## Log de execução

### Etapa 0 — CI
- Arquivos: `.github/workflows/tests.yml` (novo), `docs/plano-documentacao-estruturada.md` (novo).
- Testes novos: nenhum (etapa não toca código de aplicação).
- Suíte local (mesmo comando/env do CI: `./gradlew test --no-daemon`,
  `SPRING_PROFILES_ACTIVE=test`): **106/106 passando, 0 skipped, 0 falhas,
  0 erros**.
- Linha de base esclarecida: o "108" reportado na Fase 0 e em resumos
  anteriores da sessão era uma contagem informal (de memória/incremental),
  não uma reexecução exata. A contagem real, cruzada por dois métodos
  (agregação do XML do Gradle + `grep -c '@Test'` no código-fonte de teste),
  é **106** desde o commit `9dbb4d1` (nenhum teste foi alterado depois
  disso até aqui). Não há teste `@Disabled`/skipped. Nenhuma regressão —
  foi erro de relato, não de código.
