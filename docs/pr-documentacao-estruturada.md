# PR: Documentação Estruturada (pirâmide ISO 9001 / SIPOC / RACI / APQC)

Branch: `feature/documentacao-estruturada` → `feature/melhorias-em-todo-projeto`
(decisão 2026-10-10 — ver "Base do PR" abaixo). **Aberto como DRAFT**
(2026-10-10).

## Base do PR (decisão 2026-10-10)

A `feature/melhorias-em-todo-projeto` nunca foi totalmente integrada à
`main` — há uma **PR #10 aberta** (desde 2026-10-02) trazendo toda a
fundação (auth/JWT, RAG, documentos versionados, chat, categorias) que
esta branch depende inteiramente. Apontar nossa PR direto pra `main`
hoje duplicaria essa fundação inteira no diff (216 arquivos/+17828
linhas, contra 114 arquivos/+10279 linhas apontando pra
`feature/melhorias-em-todo-projeto`) e pediria ao time pra revisar de
novo um código já pendente de revisão na PR #10.

**Decisão**: esta PR fica **empilhada sobre a PR #10** — base =
`feature/melhorias-em-todo-projeto`, não `main`. Depois que a PR #10 for
aceita e mergeada:
- Se o merge for por **merge commit** (não squash): só reapontar a base
  desta PR pra `main` no GitHub — sem rebase, o histórico já é
  ancestral de `main` nesse ponto.
- Se o merge for por **squash**: esta branch vai precisar de um
  `git rebase`/`git merge` novo sobre a `main` pós-squash antes de
  reapontar a base (os commits da melhorias teriam SHAs diferentes).

Commits novos do time na `feature/melhorias-em-todo-projeto` desde que
esta branch foi criada: nenhum (confirmado via `git fetch` + `git log`,
2026-10-10) — sem conflito a resolver nesta sincronização.

## Resumo da funcionalidade

Adiciona, de forma **100% aditiva**, uma camada de documentação
estruturada por cima do fluxo de documentos já existente:

- **Metadados de processo**: tipo de documento (POLÍTICA, PROCESSO,
  PROCEDIMENTO, ...), hierarquia de macroprocesso/processo-pai, áreas
  participantes (join table informativa, não é controle de acesso),
  status de ciclo de vida (NAO_CLASSIFICADO/VIGENTE/EM_ELABORACAO/
  EM_REVISAO/OBSOLETO), confidencialidade.
- **Conteúdo estruturado versionável** (jsonb): DTOs tipados para
  PROCESSO (SIPOC) e PROCEDIMENTO (passo a passo + RACI), validados,
  renderizados para HTML de forma determinística (sem LLM) para
  exibição/impressão.
- **Criação/edição assistida por chat**: uma tool leve (`anthropic`
  apenas, por ora) propõe a estrutura e grava um rascunho; a confirmação
  do usuário aplica o conteúdo estruturado ao documento, via worker
  assíncrono (não bloqueia a resposta do chat).
- **Governança**: cálculo de `proximaRevisao` (data de vigência +
  periodicidade), filtro de listagem por revisão vencida (documentos
  OBSOLETO nunca contam como vencidos).
- **RAG com ciclo de vida**: metadados de processo gravados em cada
  chunk indexado; filtro (pré e pós-busca) que esconde documentos não-
  VIGENTES por padrão nas buscas do chat e do endpoint de pesquisa —
  incluindo para ADMIN, que continua com acesso irrestrito por
  categoria mas passa a ver só o vigente por padrão; uma tool dedicada
  (`buscarDocumentosIncluindoHistorico`) sempre dá acesso explícito ao
  histórico completo, com a flag ligada ou não.
- **Reindexação em massa**: endpoint ADMIN para reprocessar todos os
  documentos ativos (ex.: ao ligar o filtro de ciclo de vida pela
  primeira vez, para que nenhum chunk fique com metadado incompleto).
- **Reindexação automática**: qualquer mudança de categoria, status de
  ciclo de vida, ou confirmação de conteúdo estruturado dispara
  reindexação do documento (sem incrementar versão quando o conteúdo
  não muda), mantendo os metadados do chunk sempre sincronizados com o
  banco.

**Categoria continua sendo o único controle de acesso.** Toda a
taxonomia nova (macroprocesso, tipo, status de ciclo de vida, etc.) é
só metadado — não concede nem restringe acesso por si só.

## Flags (ambas desligadas por default)

| Flag | Variável de ambiente | Default | Controla |
|---|---|---|---|
| `bdocs.documentacao-estruturada.enabled` | `DOCUMENTACAO_ESTRUTURADA_ENABLED` | `false` | Toda a UI/API/tool de conteúdo estruturado, hierarquia de macroprocesso, endpoint de status de ciclo de vida, filtro de revisão vencida. |
| `bdocs.rag.filtros-ciclo-vida.enabled` | `RAG_FILTROS_CICLO_VIDA_ENABLED` | `false` | Só a leitura/filtro de ciclo de vida na busca (RAG — chat e endpoint de pesquisa). A gravação dos metadados no chunk e os gatilhos de reindexação por metadado ficam **sempre ligados** (aditivo, decisão B4) — não dependem desta flag. |

Propriedade complementar: `bdocs.documentacao-estruturada.provedores`
(lista, ex. `anthropic`) — em quais provedores de chat a tool
estruturada fica disponível. Hoje só `anthropic` (Gemini falha com
function calling de POJO aninhado — Etapa 10, documentado no plano).

**Com as duas flags desligadas: zero mudança de comportamento** em
relação ao estado atual de produção — nenhuma rota nova responde,
nenhum filtro de busca é aplicado, nenhuma tool nova aparece pro
modelo. Confirmado por teste dedicado em cada etapa que introduziu uma
rota/tool/filtro condicional à flag.

## Migrations incluídas (V9–V14, todas aditivas)

| Migration | O que faz |
|---|---|
| `V9__add_metadados_processo_documento.sql` | 9 colunas nullable em `documento` (tipo, status de ciclo de vida, confidencialidade, datas, etc.), com `DEFAULT` para as linhas existentes. |
| `V10__create_macroprocesso_e_hierarquia.sql` | Tabela `macroprocesso` + `macroprocesso_id`/`processo_pai_id` em `documento` (FK `ON DELETE SET NULL`). |
| `V11__create_documento_area_participante.sql` | Tabela de junção `documento_area_participante` (FK `documento_id` `ON DELETE CASCADE`, FK `categoria_id` `RESTRICT`, `UNIQUE(documento_id, categoria_id)`) — informativa, não afeta controle de acesso. |
| `V12__add_conteudo_estruturado.sql` | Coluna jsonb `conteudo_estruturado` + `versao_schema` em `documento_versao`. |
| `V13__add_geracao_assincrona_rascunho.sql` | Colunas de estado assíncrono em `documento_rascunho` (status, tentativas, erro, conteúdo estruturado proposto). |
| `V14__add_metadados_rag_documento_embedding.sql` | 5 colunas nullable em `documento_embedding` (categoria, tipo, status de ciclo de vida, macroprocesso, confidencialidade) — cobre banco já existente; `EmbeddingConfig` já cria essas colunas num banco novo. |

Nenhuma migration remove ou renomeia coluna/tabela existente, nem
altera tipo de coluna existente. `origin/main` não tem nenhuma
migration própria (diretório `db/migration` não existe lá) — **sem
colisão de número de versão do Flyway** nesta sincronização.

## Novos endpoints REST

| Método | Rota | Acesso | Atrás da flag |
|---|---|---|---|
| `GET` / `POST` / `PUT` / `DELETE` | `/macroprocessos`, `/macroprocessos/{id}` | ADMIN (escrita); autenticado (leitura) | `documentacao-estruturada` |
| `PATCH` | `/documentos/{id}/status-ciclo-vida` | ADMIN | `documentacao-estruturada` |
| `GET` | `/documentos?revisaoVencida=true` | autenticado (parâmetro novo, default `false`) | não (sempre disponível; sem o parâmetro, comportamento de sempre) |
| `POST` | `/admin/reindexacao` | ADMIN | `documentacao-estruturada` |
| `GET` | `/admin/reindexacao/status` | ADMIN | `documentacao-estruturada` |

Sem a flag `documentacao-estruturada`, as rotas condicionais a ela
simplesmente não existem (404), via `@ConditionalOnProperty` — não é
um 403, a rota não é registrada no contexto.

## Novas tools de chat (só quando `documentacao-estruturada` está ligada e o provedor é `anthropic`)

- `prepararCriacaoDocumentoEstruturado` / confirmação via
  `confirmarRascunhoPendente` (worker assíncrono aplica o conteúdo
  estruturado).
- `listarMacroprocessos` (somente leitura).
- `buscarDocumentosIncluindoHistorico` — único caminho explícito até
  documentos não-vigentes quando `filtros-ciclo-vida` está ligada;
  idêntica a `buscarDocumentos` quando está desligada.

## O que muda com as duas flags desligadas

**Nada.** Todos os pontos condicionais (rotas, tools, filtros de
busca) são aditivos e guardados por flag ou por parâmetro opt-in
(`revisaoVencida`). Confirmado por suíte de testes dedicada em cada
etapa (ex.: `MacroprocessoControllerDesligadoPorDefaultTest`,
`ReindexacaoEmMassaControllerDesligadoPorDefaultTest`,
`RagCicloVidaFiltroServiceTest.comAFlagDesligadaNuncaAplicaOPreFiltro...`,
`PesquisaServiceTest`/`RagAssistantConfigTest` — casos com a flag de
RAG desligada). Suíte completa nesta branch: **295/295 passando, 0
falhas, 0 erros.**

## Passo a passo de lançamento

1. **Variáveis de ambiente**: definir `AI_CHAT_PROVIDER=anthropic`
   (a tool estruturada só existe nesse provedor) e garantir que
   `ANTHROPIC_API_KEY` está configurada com um limite de gastos
   definido no painel da Anthropic **antes** de ligar qualquer flag
   (ver item 4).
2. **Deploy normal** (as migrations V9–V14 rodam automaticamente via
   Flyway no boot, antes de qualquer flag ser ligada — tudo aditivo,
   sem risco pro schema/dado existente).
3. **Reindexação em massa** — rodar `POST /admin/reindexacao`
   (autenticado como ADMIN) uma vez, para que todo chunk já indexado
   passe a ter os metadados novos (`categoria_id`/`status_ciclo_vida`
   gravados em `documento_embedding`). Acompanhar
   `GET /admin/reindexacao/status` até `chunksSemMetadado: 0`.
   **Sem esse passo, a flag de RAG abaixo nunca ativa o pré-filtro**
   (proteção automática contra chunk órfão — `RagCicloVidaFiltroService`)
   — a busca continua funcionando, só sem o pré-filtro otimizado (o
   filtro pós-busca já funciona mesmo com chunk órfão, só não é o mais
   performático).
4. **Ligar `RAG_FILTROS_CICLO_VIDA_ENABLED=true`** — só depois do passo
   3 confirmado. A partir daqui, chat e busca passam a esconder
   documentos não-vigentes por padrão (ADMIN incluído); o histórico
   continua acessível via `buscarDocumentosIncluindoHistorico`.
5. **Ligar `DOCUMENTACAO_ESTRUTURADA_ENABLED=true`** quando o time
   quiser expor a UI/tools de documentação estruturada aos usuários —
   independente do passo 4, pode ser ligada antes, depois, ou nunca
   (não depende da flag de RAG).
6. **Limite de gastos na Anthropic**: confirmar que o limite mensal/
   diário está configurado no painel da Anthropic antes do passo 5 —
   a tool estruturada faz chamadas reais ao modelo (geração assíncrona
   do conteúdo estruturado proposto). Ver regra de custo registrada em
   `docs/plano-documentacao-estruturada.md` (nenhuma chamada automática
   em loop; falha para e não tenta de novo).

## Frontend

Nada nesta PR — Etapas 19/20 (indicador visual de revisão vencida,
telas de documentação estruturada) ainda não começaram, e a migração
do frontend para Tailwind está planejada para acontecer antes, numa
branch própria.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
