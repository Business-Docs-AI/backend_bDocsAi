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

## Regra de custo da API da Anthropic (2026-10-10, vale para todo o restante do projeto)

Motivo: o consumo de créditos saiu do controle esperado (2 recargas em 1 dia,
quando antes uma recarga durava uma semana) — investigações empíricas
repetidas (Etapas 10/11/11b/Haiku) e testes manuais de ponta a ponta somaram
muitas chamadas reais de ~14-16k tokens de saída cada.

1. **Nenhuma chamada real à API da Anthropic sem autorização explícita do
   usuário naquele momento.** Antes de pedir, informar quantas chamadas serão
   feitas e a estimativa de tokens.
2. Nunca repetir testes de coisas já validadas.
3. Testes automatizados e CI continuam sem chave (modelo mockado) — nada
   muda aqui.
4. Nada de loops, novas tentativas ou "só mais uma rodada" com a API real.
   Falhou, parar e reportar — nunca tentar de novo por conta própria.
5. Etapas 14 em diante não devem precisar da API da Anthropic. Se alguma
   precisar, parar e pedir autorização antes.

Mecanismo criado para viabilizar testes manuais de ponta a ponta sem gastar
créditos: modo `modelo-fake` do worker de geração assíncrona (ver Etapa
13.6) — devolve um documento de exemplo fixo e válido sem nunca chamar a
Anthropic. Ver "Total de chamadas reais à API desde o início da Etapa 13"
no fechamento da Etapa 13.6 para o registro de uso até aqui.

## Migração do frontend para Tailwind CSS (decisão 2026-10-10)

O frontend inteiro (`businessDocsAi-frontend`) vai migrar de MUI/CSS puro
para Tailwind CSS. Decisão registrada aqui porque afeta o cronograma:
**a migração acontece numa branch e PR próprios, separados da
documentação estruturada, e ANTES das Etapas 19 e 20** (que passarão a
ser escritas já em Tailwind).

Levantamento (2026-10-10, read-only, sem nenhuma alteração no repo do
frontend): 1 arquivo CSS puro (`global.css`, 17 linhas, irrelevante);
15 arquivos/130 ocorrências de `sx=` do MUI; zero `styled()`/`makeStyles`;
zero `style={{}}` inline; tema central em `theme/theme.ts` (paleta,
tipografia, overrides de Button/AppBar/Paper) mapeável pra
`tailwind.config`; 18 de 40 arquivos (45%) importam `@mui`, sempre por
import profundo (não barrel) — bom sinal pra migração incremental; ~30
componentes MUI distintos em uso (Box/Typography/Stack/Button são os
mais comuns, família Dialog completa, AppBar/Tabs/Drawer/Menu,
Alert/Snackbar/CircularProgress/Tooltip, CssBaseline, ~25 ícones).
Stack atual (React 18.3.1/Vite 5.4.10/MUI 6.1.6/TS 5.6.3) compatível
com Tailwind v4 (`@tailwindcss/vite`, recomendado) ou v3.

Riscos de convivência Tailwind+MUI durante a transição: preflight do
Tailwind vs. `CssBaseline` do MUI (mitigar desligando o preflight
enquanto o MUI ainda existir); especificidade (Emotion/MUI geralmente
vence utility classes — mitigar com `important` escopado ou aplicando
Tailwind primeiro em componentes sem MUI por baixo); ordem de injeção
de CSS no Vite (Tailwind depois do Emotion).

Duas opções de escopo, ainda não decididas entre si:
- **A — Tailwind substitui CSS puro/avulso, MANTÉM os componentes MUI**:
  esforço baixo-médio, incremental, baixo risco (não há `styled()` nem
  CSS puro significativo pra desembaraçar).
  - **B — Tailwind substitui tudo, REMOVE o MUI**: esforço alto —
  reconstrução de ~30 componentes (vários com lógica de portal/
  posicionamento, não só estilo) com uma lib de primitivos acessíveis
  + Tailwind; sem testes automatizados no frontend, risco alto de
  regressão funcional, não só visual.

Migração incremental proposta: 1 PR por página/componente, screenshot
antes/depois de cada tela (sem teste automatizado, validação visual é
obrigatória), ordem sugerida por risco crescente (tema → páginas
simples → páginas com formulário → Chat, a mais complexa).

## Flags

| Flag | Default | Controla |
|---|---|---|
| `bdocs.documentacao-estruturada.enabled` | `false` | Etapas 1–13, 17 (parte do cálculo de `proxima_revisao`), 18–20 |
| `bdocs.rag.filtros-ciclo-vida.enabled` | `false` | Só a Etapa 16 (leitura/filtro dos metadados na busca). A gravação dos metadados no chunk (Etapa 14) e os gatilhos de reindexação por metadado (Etapa 17) ficam **sempre ligados** (aditivo, decisão B4). |

Propriedade adicional: `bdocs.documentacao-estruturada.provedores` (lista,
ex. `gemini,anthropic`) — define em quais provedores de chat a tool
estruturada fica disponível (decisão A3).

**Decisão de escopo (2026-10-09, pós-Etapa 10)**: por ora, a tool estruturada
só é habilitada para `anthropic` — `bdocs.documentacao-estruturada.provedores=anthropic`.
Gemini (`gemini-3.8-flash` via `langchain4j-google-ai-gemini:1.18.0`) falha de
forma determinística em qualquer function calling via `AiServices` (ver Log,
Etapa 10) — ficou fora do escopo desta entrega, sem alterar dependência nem
tentar corrigir. O provedor **padrão do código continua `gemini`**
(`ChatModelConfig`/`application.yaml` inalterados) — a troca para `anthropic`
em produção é só **configuração de ambiente** (`AI_CHAT_PROVIDER=anthropic`),
não uma mudança de código. Isso é um passo operacional de deploy a lembrar,
não uma etapa do plano. A Etapa 11 (orçamento de tokens) também será medida
só com Anthropic.

**Nota de operação (B4)**: antes de ligar `bdocs.rag.filtros-ciclo-vida.enabled`
em qualquer ambiente, rodar a reindexação em massa (Etapa 15) primeiro.

## Problema separado: Gemini + tool-calling via AiServices (fora do escopo desta entrega)

Registrado na Etapa 10, **não é um problema desta feature** — é uma
incompatibilidade pré-existente entre `gemini-3.8-flash` (modelo
"thinking") e `AiServices` do `langchain4j-google-ai-gemini:1.18.0`:
qualquer chamada de ferramenta (nested DTO ou tool simples de parâmetros
escalares, como a já existente `prepararCriacaoDocumento`) falha com
`400 Function call is missing a thought_signature`. Isso significa que o
chat de produção **já quebraria hoje** para qualquer tool-calling se
`AI_CHAT_PROVIDER=gemini` fosse usado — não depende desta entrega.
Nenhuma correção tentada (fora do escopo). Sugestões futuras para avaliar
fora deste plano: (a) trocar o provedor **padrão** do código de `gemini`
para `anthropic` em `ChatModelConfig`/`application.yaml` (hoje o padrão é
gemini só por ser gratuito, mas é o que quebra); (b) atualizar
`langchain4j-google-ai-gemini` para uma versão que propague
`thought_signature` corretamente, se/quando existir. Nenhuma das duas
ações faz parte deste plano — só registrado para decisão futura do
usuário.

## Decisões de modelagem originais (1–12, aprovadas antes do plano)

1. **STATUS**: campo novo `status_ciclo_vida` (`EM_ELABORACAO | EM_REVISAO | VIGENTE | OBSOLETO`) — **não** toca `status_indexacao` (semântica diferente, é sobre indexação). **Não** usa o valor "RASCUNHO" (confundiria com `documento_rascunho`, que continua significando proposta da IA pendente de confirmação). Documentos existentes → `VIGENTE` via migration. Documentos confirmados pelo fluxo atual de chat → `VIGENTE` (preserva o comportamento de hoje). Fluxo de revisão/aprovação fica para o futuro (ver "Explicitamente adiado").
2. **TIPO**: campo novo `tipo_documento` (`POLITICA | PROCESSO | PROCEDIMENTO | INSTRUCAO | REGISTRO | NAO_CLASSIFICADO`). Existentes → `NAO_CLASSIFICADO`.
3. **CATEGORIA = ÁREA DONA**: nenhuma entidade "área" nova — `categoria_id` já é a área dona e continua sendo o **único** controle de acesso (`CategoriaAccessService` não muda). `areas_participantes`: tabela N:N documento↔categoria, **apenas informativa** — nunca concede acesso em hipótese alguma.
4. **HIERARQUIA**: tabela nova `macroprocesso` (`id`, `nome`, `descricao`) + FK nullable `macroprocesso_id` em `documento`; `processo_pai_id` nullable em `documento`, autorreferenciando `documento`.
5. **ARMAZENAMENTO**: colunas nullable em `documento` para os metadados filtráveis (`tipo`, `status_ciclo_vida`, `macroprocesso_id`, `processo_pai_id`, `dono_processo`, `aprovador`, `data_vigencia`, `proxima_revisao`, `periodicidade_revisao`, `confidencialidade`, `tags`). Conteúdo estruturado em `conteudo_estruturado` (jsonb, nullable) + `versao_schema`, nas tabelas `documento`, `documento_versao` e `documento_rascunho`, versionado/restaurado junto com o HTML. Etapas/regras/exceções/RACI ficam dentro do jsonb, sem tabelas relacionais por enquanto. `conteudo_html` continua obrigatório e sempre preenchido.
6. **SAÍDA ESTRUTURADA**: como a geração acontece via `@Tool`, tool **nova** (`prepararCriacaoDocumentoEstruturado`/`...Atualizacao...`) recebendo um POJO/record (langchain4j gera o schema), registrada só com a flag ligada — as tools atuais continuam existindo e funcionando sem alteração. Validação do objeto no servidor (Bean Validation). HTML renderizado no **servidor**, de forma determinística, a partir do JSON (com `h2`/`h3` compatíveis com `HtmlSectionSplitter`), passando pelo `HtmlSanitizerService` como hoje. Fluxo propor → confirmar em turno separado mantido. Avaliar se `maxTokens(8192)` comporta um documento completo (Etapa 11). Verificar function calling com POJO aninhado nos 3 provedores (Etapa 10) — se algum não suportar, a flag não ativa o caminho novo para esse provedor.
7. **RAG**: adicionar `categoria_id`, `tipo_documento`, `status_ciclo_vida`, `macroprocesso_id`, `confidencialidade` (+ ID do item atômico quando houver) aos metadados do chunk. Filtro ANTES da busca (`Filter` do langchain4j sobre as colunas), **mantendo** o filtro pós-busca atual como rede de segurança. Chunks sem o metadado `status_ciclo_vida` (legados) tratados como `VIGENTE` — nenhum documento atual pode sumir do RAG. Mudança de categoria/status/confidencialidade dispara reindexação. Reindexação em massa só manual/opt-in, nunca automática. Confidencialidade: só metadado + filtro preparado nesta entrega, sem regra de acesso nova.
8. **DIAGRAMA**: Mermaid aprovado como dependência do frontend, mas como **última** etapa do plano. Backend gera o texto Mermaid a partir das etapas.
9. **GOVERNANÇA**: sem e-mail. Só cálculo de `proxima_revisao`, indicador visual de "revisão vencida" na UI e filtro de listagem. `pendencias[]` visíveis no documento.
10. **FORA DO ESCOPO**: pacotes `conversation` e `uploadFiles` (legado, desconectado), `permission_id` do usuário.
11. **CI**: workflow do GitHub Actions rodando testes com Postgres+pgvector como service — Etapa 0, concluída.
12. **FEATURE FLAG**: propriedade única `bdocs.documentacao-estruturada.enabled` (default `false`). Com a flag desligada, o sistema se comporta **exatamente** como hoje (migrations aplicadas, sem efeito funcional).

## Índice das etapas

| # | Etapa | Status |
|---|---|---|
| 0 | CI (workflow Postgres+pgvector) | ✅ concluída |
| 1 | Feature flag `documentacao-estruturada` | ✅ concluída |
| 2 | Metadados escalares em `documento` | ✅ concluída |
| 3 | Hierarquia de processo (`macroprocesso` + `processo_pai_id`) | ✅ concluída |
| 4 | Macroprocesso — tool de listagem + CRUD ADMIN | ✅ concluída |
| 5 | Endpoint ADMIN de mudança de `status_ciclo_vida` | ✅ concluída |
| 6 | Áreas participantes (join table informativa) | ✅ concluída |
| 7 | Conteúdo estruturado versionável (jsonb) | ✅ concluída |
| 8 | DTOs estruturados + validação (Bean + semântica) | ✅ concluída |
| 9 | Renderizador HTML determinístico | ✅ concluída |
| 10 | Investigação: function calling com POJO aninhado | ✅ concluída — Gemini excluído do escopo (quebra genérica, não é do DTO); Anthropic 2/5 sucesso completo, 3/5 esgotaram orçamento de tokens (ver Log) |
| 11 | Investigação: orçamento de tokens | ✅ concluída (11 + 11b) — meta de <6.000 tokens/<60s **NÃO atingida**; melhor resultado ~13.9k tokens/~129s com prompt mais rigoroso (sem mudar schema). Correção de `maxTokens`/timeout fica para a Etapa 13, aguardando decisão sobre geração assíncrona (ver Log) |
| 12 | Prompt v2 (composto sobre o v1) | ⏳ pendente — já tem o texto validado na Etapa 11b (V1: uma única chamada + anti-redundância + concisão) |
| 13.1 | Migrações aditivas p/ geração assíncrona (`StatusRascunho` +2 valores, `conteudo_html` nullable, 4 colunas novas) | ✅ concluída — **substitui a antiga "Etapa 13" única** (ver "Proposta — geração assíncrona" abaixo) |
| 13.2 | `ChatModel` dedicado ao worker de geração (maxTokens/timeout próprios, não toca o `chatModel` do chat interativo) | ✅ concluída |
| 13.3 | Tool leve de solicitação (`solicitarGeracaoDocumentoEstruturado`/`...Atualizacao...`) + evento de disparo + R1 nas tools legadas + confirmação estruturada (B3) | ✅ concluída |
| 13.4 | Worker de geração (listener + job de rede de segurança, reaproveitando validator/renderer existentes) | ✅ concluída |
| 13.5 | Endpoint de leitura ampliado (`status`/`erroGeracao` aditivos) + confirmação só em `PENDENTE` | ✅ concluída (a regra de confirmação já tinha sido feita na 13.3, junto do resto de R1) |
| 13.6 | Frontend: polling (reaproveitando `getPendingDraft` existente) + estado "gerando"/erro no painel | ✅ concluída |
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
- **C2**: metadados precisam de caminho de entrada —
  (a) o DTO estruturado (Etapa 8) tem um bloco de metadados
  (`tipoDocumento`, `macroprocessoId`, `processoPaiId`,
  `areasParticipantes[]`, `donoProcesso`, `aprovador`,
  `periodicidadeRevisao`, `confidencialidade`, `tags`);
  (b) esses metadados viajam no `conteudo_estruturado` do rascunho e são
  aplicados às colunas de `documento` em `confirmarRascunhoPendente`
  (Etapa 13), validando que os IDs existem e que o usuário tem acesso à
  categoria (área dona — acesso completo; áreas participantes — só
  existência do ID, por serem informativas);
  (c) macroprocesso: tool `listarMacroprocessos` (padrão de
  `listarMinhasCategorias`) + CRUD ADMIN simples (Etapa 4) — caminho
  escolhido por ser o menos intrusivo (sem fluxo de proposta/confirmação
  pelo chat, que exigiria uma tabela de rascunho nova só pra isso);
  (d) mudança de status: endpoint ADMIN (Etapa 5), não tool de chat —
  gatilho necessário pra Etapa 17 ter o que acionar.
- **C3**: consistência JSON×HTML — se um documento com
  `conteudo_estruturado` for atualizado pelo fluxo legado de HTML, a nova
  versão grava `conteudo_estruturado = NULL` (o JSON antigo permanece só
  na versão anterior). Essa regra cobre os DOIS caminhos: `atualizar()`
  (REST) e `confirmarRascunhoPendente` de um rascunho ATUALIZAR sem
  estruturado (chat legado) — teste obrigatório nos dois. `restaurarVersao`
  também restaura `conteudo_estruturado`/`versao_schema` da versão
  escolhida.
- **C4**: a mesma regra "vigente por padrão, sem metadado = vigente" vale
  também em `PesquisaService` (endpoint `/documentos/busca`), não só no
  RAG do chat.
- **C5**: segunda flag dedicada ao RAG (ver tabela de flags acima).

## Decisões para o prompt v2 (Etapa 12, registradas em 2026-10-09 — ainda não implementadas)

- **Decisão vs. exceção**: decisão = desvio PREVISTO no fluxo normal, com
  caminho definido (devolver, recusar, aprovar) — modelar como `EtapaDTO`
  com `decisao`. Exceção = situação FORA do fluxo normal — modelar como
  `ExcecaoDTO`. (A Etapa 10 mostrou as duas interpretações sendo usadas
  pelo modelo para o mesmo caso de recusa por CNPJ/rasura; esta decisão
  resolve a ambiguidade a favor de "decisão" quando há um caminho de
  retorno claro no fluxo.)
- **Confidencialidade não informada** → assumir `INTERNO` **e** registrar
  em `pendencias` (não deixar `null` silenciosamente) — a execução 3 da
  Etapa 10 já fez isso espontaneamente e funcionou bem.
- **Responsáveis**: derivar o PAPEL do contexto quando possível (ex.:
  "qualquer analista do financeiro" → "Analista Financeiro"); nome de
  pessoa nunca é aceito como responsável, nem em RACI nem em etapas.
- **Instrução de concisão**: descrições curtas e objetivas em cada campo
  de texto livre — confirmado pela Etapa 11 que isso reduz tokens de
  saída (~4%), latência (~15%) e chamadas redundantes da ferramenta
  (20% vs. 50%), sem contrapartida observada.
- **V1 da Etapa 11b aprovada como o prompt base**: instrução de "uma
  única chamada" (chamar a ferramenta duas vezes é erro) + retorno da
  tool reforçando "não chame de novo" + anti-redundância nas listas
  macro (sipoc/riscosControles/indicadores/sistemasFerramentas não
  repetem o que já está em alguma etapa) — eliminou 100% das chamadas
  duplas nos testes (0/10). `RaciEntryDTO.responsavel` **NÃO** será
  removido do schema (testado na Etapa 11b, Variação 2 — não trouxe
  ganho mensurável, só complexidade).

## Investigação: modelo dedicado para o worker de geração (Haiku, 2026-10-09)

5 execuções com `claude-haiku-4-5-20251001` (mesmo prompt V1, mesmo
schema de produção, mesmo material/gabarito), comparado com Sonnet V1
(Etapa 11b: 5/5 sucesso, 5/5 validador limpo, ~13.897 tokens méd.,
~129s méd.):

| | Chamou a tool | Validador limpo | Tokens saída (quando chamou) | Latência (quando chamou) |
|---|---|---|---|---|
| Haiku (5 execuções) | **2/5 (40%)** | 0/2 (ambas com 1 e 3 erros de referência etapa↔excecao) | ~5.210 (dentro da meta de <6.000!) | ~48,5s (dentro da meta de <60s!) |
| Sonnet V1 (referência, Etapa 11b) | 5/5 | 5/5 | ~13.897 | ~129,0s |

Quando o Haiku chama a ferramenta, o resultado é rápido e pequeno — bateria
a meta de tokens/latência que a Etapa 11b não conseguiu com o Sonnet.
**Mas em 3/5 execuções (60%) o Haiku NÃO chamou a ferramenta** — em vez
disso, respondeu com texto fazendo perguntas de esclarecimento ao
usuário (ex.: "Deixa eu fazer algumas perguntas rápidas..."), ignorando
a instrução explícita do prompt de usar `[A DEFINIR]`/`pendencias` e
chamar a ferramenta mesmo com lacunas. **Isso é desqualificante para um
worker sem usuário presente** (a arquitetura assíncrona aprovada não tem
ninguém pra responder a perguntas no meio da geração) — e, nas 2
execuções em que chamou, ainda cometeu mais erros de referência
(etapa↔exceção) que o Sonnet em toda a Etapa 11b (1 erro em 11
execuções do Sonnet vs. 1 e 3 erros nas 2 execuções do Haiku que
completaram). **Conclusão: mantido o modelo atual (Sonnet) no worker**,
conforme a regra já definida pelo usuário ("se a qualidade cair, mantém
o modelo atual") — o Haiku é mais rápido/barato, mas não é confiável o
suficiente pra rodar sem supervisão.

## Proposta: geração assíncrona (Etapa 13.1–13.6, substituindo a antiga Etapa 13 única)

Decisão do usuário (2026-10-09): a latência de ~130s é estrutural (não
compensa perder qualidade/conteúdo real do processo pra tentar cortar
pra <60s — a Etapa 11b mostrou que o teto estrutural real fica por volta
de ~130s/~14k tokens com conteúdo completo). Caminho escolhido: geração
**assíncrona** — o chat responde rápido ("gerando...") e a proposta
aparece quando o worker terminar, em vez do usuário esperar ~2min
olhando uma tela parada (e sem depender do timeout de 60s do
frontend/60s do cliente HTTP da Anthropic pra essa chamada pesada).

### Padrões reaproveitados (nada novo sendo inventado)

O projeto já tem exatamente o par de padrões necessário, usado hoje para
indexação: `IndexacaoListener` (`@TransactionalEventListener(AFTER_COMMIT)`
+ `@Async("indexacaoExecutor")`, reação imediata) **e**
`ReindexacaoJob` (`@Scheduled`, rede de segurança que reprocessa
`PENDENTE`/`ERRO`). A proposta usa a MESMA dupla de padrões para a
geração estruturada, só com nomes/executor dedicados novos.

### 13.1 — Migrações aditivas

- `StatusRascunho` ganha `GERANDO` e `ERRO_GERACAO` (enum, aditivo — os
  3 valores atuais `PENDENTE`/`CONFIRMADO`/`DESCARTADO` não mudam de
  significado; o fluxo legado nunca entra nos dois novos estados).
- `documento_rascunho.conteudo_html` passa de `NOT NULL` para nullable
  (`ALTER COLUMN ... DROP NOT NULL`) — só o caminho novo deixa nulo
  temporariamente enquanto `status=GERANDO`; o caminho legado (e o
  `@NotBlank` da camada de aplicação nesse caminho) continua sempre
  preenchendo, sem mudança de comportamento.
- 2 colunas novas em `documento_rascunho`: `erro_geracao` (text,
  nullable) e `tentativas_geracao` (int, not null, default 0).
- **Risco**: baixo — migração pura, nenhum dado existente é tocado,
  relaxar `NOT NULL` nunca quebra uma linha que já tinha valor.
- **Testes**: migration up roda sobre uma base com rascunhos legados
  existentes sem alterar nenhum deles; nenhum rascunho antigo some ou
  muda de status; tentativa de inserir um rascunho novo sem
  `conteudo_html` só funciona pelo caminho novo (teste negativo no
  caminho legado continua exigindo o campo, pela validação da
  aplicação).
- **Reversão**: `ALTER COLUMN conteudo_html SET NOT NULL` (só segura se
  nenhum rascunho ficou com ele nulo — documentar a checagem antes de
  reverter em produção) + remover as 2 colunas novas e os 2 valores do
  enum (sem uso, se a feature for desfeita antes de qualquer rascunho
  chegar a usá-los).

### 13.2 — `ChatModel` dedicado ao worker

Bean novo `chatModelGeracaoEstruturada`, `@ConditionalOnProperty` (flag
ligada e `anthropic` em `provedores`), com `maxTokens`/`timeout`
próprios e configuráveis:
- `bdocs.documentacao-estruturada.geracao.max-tokens` (default `16384`,
  validado na Etapa 11b).
- `bdocs.documentacao-estruturada.geracao.timeout-segundos` (default
  `240`, com margem sobre o pior caso medido ~150s).

O bean `chatModel` existente (chat interativo, usado por TODAS as tools
de hoje) **não muda em nada** — continua com `maxTokens(8192)` e timeout
padrão. Esse é o ponto chave de por que o timeout do frontend não
precisa mudar (ver seção de timeouts abaixo). Aproveitar esse arquivo
pra corrigir o comentário desatualizado sobre "thinking consumir a
cota" (achado da Etapa 11 — thinking está desligado).

- **Risco**: baixo — bean novo, isolado, não altera a assinatura nem o
  comportamento do bean existente.
- **Testes**: contexto sobe com a flag ligada e desligada (padrão já
  usado em `ContextLoadsComFlagDocumentacaoEstruturadaLigadaTest`); teste
  de regressão garantindo que o bean `chatModel` continua com
  `maxTokens=8192` (nenhuma mudança de comportamento no chat
  interativo).

### 13.3 — Tool leve de solicitação

Nova classe `DocumentoEstruturadoTools` (padrão de `MacroprocessoTools`:
`@Component`, `@ConditionalOnProperty`, só exposta na lista de tools do
`AiServices` quando `FeatureFlags.documentacaoEstruturadaHabilitadaPara(provedorAtual)`
for true — isso já cobre o caso de o chat estar rodando num provedor
onde nem a tool leve deveria aparecer).

Dois métodos **leves** (sem o JSON completo como argumento):
- `solicitarGeracaoDocumentoEstruturado(String tituloSugerido, String instrucoesAdicionais)`
- `solicitarAtualizacaoDocumentoEstruturado(String documentoIdAlvo, String instrucoesAdicionais)`

Cada um: valida acesso à categoria (igual a `prepararCriacaoDocumento`
hoje), cria o `RascunhoDocumentoEntity` com `status=GERANDO`,
`conteudoHtml=null`, `tentativasGeracao=0`; responde ao usuário
confirmando que a geração começou; publica
`GeracaoEstruturadaSolicitadaEvent(rascunhoId)` (mesmo padrão de
`DocumentoAlteradoEvent`).

**Material-fonte da conversa — decisão de desenho**: NÃO vem como
argumento da tool. Passar o material como parâmetro arriscaria o modelo
resumir/reformular o material ao "repetir" ele num campo de tool (perda
de fidelidade, e tokens extra pagos duas vezes). Em vez disso, o worker
(13.4) lê o **histórico persistido da conversa** (mensagens já salvas no
banco, pelo `conversaId` do rascunho) diretamente — texto integral,
verbatim, sem depender do que a tool recebeu.

- **Risco**: baixo — tool nova e isolada; `prepararCriacaoDocumento`
  (fluxo legado) não é tocado.
- **Testes**: tool só aparece com flag+provedor corretos (padrão
  `MacroprocessoToolsTest`/`...DesligadoPorDefaultTest`); rascunho criado
  com os campos certos; evento só publicado depois do commit.

### 13.4 — Worker de geração

- `GeracaoEstruturadaListener` (`@Async("geracaoEstruturadaExecutor")` +
  `@TransactionalEventListener(AFTER_COMMIT)`) reage imediatamente ao
  evento da 13.3.
- `GeracaoEstruturadaJob` (`@Scheduled`, mesmo padrão do
  `ReindexacaoJob`) varre rascunhos em `GERANDO` há mais de N minutos
  (`bdocs.documentacao-estruturada.geracao.timeout-gerando-minutos`,
  default 5) — rede de segurança para evento perdido ou processo
  reiniciado no meio.
- `GeracaoEstruturadaService` faz o trabalho real: monta o material a
  partir do histórico da conversa; chama `chatModelGeracaoEstruturada`
  via `AiServices` com a tool REAL (`DocumentoEstruturadoDTO` completo,
  prompt V1); valida (Bean Validation + `DocumentoEstruturadoValidator`,
  ambos já existentes, sem alteração); se inválido, devolve os erros ao
  modelo no MESMO laço fechado (sem usuário no meio — decisão A2 adaptada
  a um worker) até `tentativas_geracao` atingir
  `bdocs.documentacao-estruturada.geracao.max-tentativas` (default 3).
  - **Sucesso**: renderiza o HTML (`EstruturaDocumentoHtmlRenderer`, já
    existe, sem alteração), grava `conteudoHtml`/`conteudoEstruturado`/
    `versaoSchema`, muda `status` para `PENDENTE` — a partir daqui a
    confirmação funciona EXATAMENTE como hoje.
  - **Falha** (tentativas esgotadas, exceção da API, `finishReason=LENGTH`
    sem chamada de ferramenta utilizável): grava `erroGeracao` com
    mensagem clara, muda `status` para `ERRO_GERACAO`.
- **Risco**: médio — é a peça de lógica nova mais substancial, mas
  reaproveita validator/renderer/entidades existentes sem alterá-los.
- **Testes**: sucesso de primeira; sucesso após 1 correção (laço A2);
  esgotamento de tentativas → `ERRO_GERACAO`; exceção da API →
  `ERRO_GERACAO`; nenhuma mudança observável no fluxo síncrono legado
  (mesmo teste de regressão do `DocumentoServiceTest` de sempre).

### 13.5 — Endpoint de leitura + regra de confirmação

- `PropostaDocumento` (DTO do `GET /chat/conversas/{id}/rascunho-pendente`,
  **já existe**) ganha campos aditivos: `status`
  (`GERANDO|PENDENTE|ERRO_GERACAO`) e `erroGeracao` (nullable) — contrato
  antigo preservado, só cresce (mesmo padrão de compat usado em
  `DocumentoResponseDTO` ao longo deste plano).
- Confirmação (`confirmarRascunhoPendente`/rota REST) passa a checar
  explicitamente `status == PENDENTE` antes de aplicar — hoje isso é
  implícito (só existe um estado possível); com os novos estados, uma
  tentativa de confirmar em `GERANDO`/`ERRO_GERACAO` precisa devolver um
  erro claro em vez de aplicar algo incompleto/inválido.
- **Risco**: baixo — campo novo opcional, nenhum consumidor existente
  quebra.
- **Testes**: testes de controller existentes continuam passando sem
  tocar nos campos novos; tentar confirmar um rascunho em
  `GERANDO`/`ERRO_GERACAO` retorna erro claro.

### 13.6 — Frontend: polling no painel existente

- `PropostaDocumento` (tipo TS) ganha `status`/`erroGeracao` (aditivo).
- `CenterContent` (`useChat.ts`) passa a tratar `status==='GERANDO'`
  mostrando um indicador de "gerando documento estruturado..." no MESMO
  painel que já existe para `documentDraft`, e `ERRO_GERACAO` mostrando
  a mensagem de erro com opção de tentar de novo.
- Ao receber a confirmação textual de "gerando" na mensagem do chat, um
  polling (`setInterval`, reaproveitando `chatService.getPendingDraft`
  **já existente** — nenhum endpoint novo) passa a checar o status a
  cada poucos segundos, parando quando virar `PENDENTE` ou
  `ERRO_GERACAO`. **Sem WebSocket/SSE**, como decidido.
- **Risco**: baixo-médio (UI nova, mas só ativa quando o backend
  sinaliza `GERANDO`, que só acontece com flag+provedor ligados).
- **Como validar**: manualmente no browser com a flag ligada — enviar
  pedido de documento estruturado, confirmar que aparece "gerando...",
  que o painel atualiza sozinho (sem reload) quando o worker termina, e
  que um erro de geração aparece de forma clara.

### Timeouts — confirmação pedida

**O timeout de 60s do frontend (`SEND_MESSAGE_TIMEOUT_MS`) NÃO precisa
mudar.** Confirmado: com a geração movida para o worker, a chamada
SÍNCRONA do turno de chat volta a ser só a tool LEVE (poucos parâmetros
escalares, sem o JSON grande) — do mesmo tamanho/latência das tools que
já existem hoje (`listarMinhasCategorias`, `prepararCriacaoDocumento`),
que já funcionam dentro do timeout atual. O `maxTokens`/`timeout` maiores
(validados na Etapa 11b: 16384/240s) passam a valer **só** no bean
dedicado do worker (13.2), nunca no bean do chat interativo — por isso
não há mais nenhum ponto do fluxo síncrono que precise de um timeout
maior que o de hoje.


## Explicitamente adiado (fora do escopo desta entrega)

- Glossário corporativo + expansão de consulta no RAG.
- Templates estruturados de POLITICA / INSTRUCAO / REGISTRO (esta entrega
  cobre só PROCESSO e PROCEDIMENTO).
- Fluxo de revisão/aprovação (EM_ELABORACAO → EM_REVISAO → VIGENTE).
- Regras de acesso por confidencialidade.
- Limpeza do código legado (`conversation`, `uploadFiles`, `permission_id`).

## Log de execução

### Etapa 0 — CI
- Arquivos: `.github/workflows/tests.yml` (novo), `docs/plano-documentacao-estruturada.md` (novo), `gradlew` (correção de permissão, mode 100644→100755).
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
- CI real: 1ª execução (`eeb596b`) falhou — `./gradlew: Permission denied`
  (exit 126), porque `gradlew` estava versionado sem bit de execução
  (nunca tinha rodado em Linux antes). Corrigido via
  `git update-index --chmod=+x gradlew` (`fa3cc75`, mudança de modo só,
  0 linhas de conteúdo). 2ª execução: ✅ sucesso — run
  [37251793303](https://github.com/Business-Docs-AI/backend_bDocsAi/actions/runs/37251793303).
- Desvio do plano: 1 commit extra de correção (`fa3cc75`) além do
  commit principal da etapa, por causa do bug de permissão do `gradlew`.

### Etapa 1 — Feature flag
- Arquivos: `config/FeatureFlags.java` (novo), `config/FeatureFlagsTest.java` (novo),
  `application.yaml` (`bdocs.documentacao-estruturada.enabled` +
  `bdocs.documentacao-estruturada.provedores`), `docs/plano-documentacao-estruturada.md`
  (completado com as 12 decisões originais + C2/C3, e este log).
- Testes novos: 4 (`FeatureFlagsTest`: default desligada/sem provedores,
  ligada sem provedor na lista, parse de lista separada por vírgula
  ignorando espaço/maiúscula, provedor nulo nunca habilita).
- Suíte completa (execução real, `./gradlew test --no-daemon --rerun-tasks`):
  **110/110 passando, 0 skipped, 0 falhas, 0 erros** (era 106 — aumentou 4,
  consistente com os testes novos).
- Desvios do plano: nenhum.

### Etapa 2 — Metadados escalares em `documento`
- Arquivos: `document/entity/TipoDocumento.java`, `StatusCicloVida.java`,
  `Confidencialidade.java` (novos enums); `db/migration/V9__add_metadados_processo_documento.sql`
  (novo); `document/entity/DocumentoEntity.java` (9 campos novos nullable);
  `document/dto/DocumentoResponseDTO.java` (9 campos novos no construtor canônico +
  construtor de compatibilidade com a assinatura antiga, pra não reescrever os 4 call
  sites de teste existentes a cada etapa que adicionar campo); `document/service/DocumentoService.java`
  (`criar()` seta `tipoDocumento`/`statusCicloVida` explicitamente — o Hibernate manda NULL
  pra campo não setado, o que bypassaria o `DEFAULT` do banco; `toResponseDTO` repassa os
  campos novos); `document/service/DocumentoServiceTest.java` (+1 teste);
  `document/entity/DocumentoMetadadosDefaultTest.java` (novo, `@SpringBootTest`, confirma o
  `DEFAULT` da migration via INSERT bruto por `JdbcTemplate`).
- Testes novos: 2 (`criarDeveDefinirTipoDocumentoNaoClassificadoEStatusCicloVidaVigentePorDefault`;
  `insertBrutoSemMencionarAsColunasNovasRecebeOsDefaultsDaMigrationV9`).
- Suíte completa (execução real): **112/112 passando, 0 skipped, 0 falhas,
  0 erros** (era 110 — aumentou 2, consistente).
- Desvios do plano: nenhum. Decisão de implementação não detalhada no
  plano original: `DocumentoResponseDTO` ganhou um construtor de
  compatibilidade (assinatura de 11 argumentos, preenchendo os campos
  novos com `null`) além do canônico, para não precisar reescrever os
  call sites de teste existentes toda vez que um campo novo for
  adicionado nas próximas etapas — o record continua 100% aditivo, só a
  forma de construí-lo ganhou um atalho extra.

### Etapa 3 — Hierarquia de processo
- Arquivos: `document/entity/MacroprocessoEntity.java`,
  `document/repository/IMacroprocessoRepository.java` (novos);
  `db/migration/V10__create_macroprocesso_e_hierarquia.sql` (novo, tabela
  `macroprocesso` + `macroprocesso_id`/`processo_pai_id` em `documento`,
  FK de `processo_pai_id` com `ON DELETE SET NULL`);
  `document/entity/DocumentoEntity.java` (2 campos novos);
  `document/dto/DocumentoResponseDTO.java` (+2 campos no construtor
  canônico, compat constructor atualizado); `document/service/DocumentoService.java`
  (`validarHierarquiaProcesso` — rejeita auto-referência e ciclo, ainda
  sem nenhum chamador em produção, pronta pra Etapa 13; `listar`/`toResponseDTO`
  agora ignoram `processoPaiId` quando o pai está soft-deletado, em lote
  pra evitar N+1); testes novos em `DocumentoServiceTest`;
  `document/repository/IMacroprocessoRepositoryTest.java` (novo).
- Testes novos: 8 (`validarHierarquiaProcesso`: nulo não consulta banco,
  rejeita auto-referência, rejeita ciclo direto, rejeita ciclo indireto
  [A→B→C→A], aceita hierarquia válida sem ciclo; `buscarPorId`/`listar`:
  pai soft-deletado nunca aparece na resposta; repositório de
  macroprocesso: salva e encontra).
- Suíte completa (execução real): **120/120 passando, 0 skipped, 0 falhas,
  0 erros** (era 112 — aumentou 8, consistente).
- Desvios do plano: nenhum.

### Etapa 4 — Macroprocesso (tool de listagem + CRUD ADMIN)
- Arquivos: `document/dto/MacroprocessoRequestDTO.java`,
  `MacroprocessoResponseDTO.java`, `document/service/MacroprocessoService.java`,
  `document/controller/MacroprocessoController.java` (`@ConditionalOnProperty`
  na flag — sem ela, a rota não existe), `document/tool/MacroprocessoTools.java`
  (idem, só leitura — criar macroprocesso não é feito pelo chat, decisão C2c);
  `ai/generation/RagAssistantConfig.java` (`ragAssistantComFerramentas` agora
  recebe `Optional<MacroprocessoTools>`, adicionado à lista de tools só
  quando presente); testes novos em `MacroprocessoServiceTest`,
  `MacroprocessoControllerSecurityTest` (flag ligada via
  `@TestPropertySource`), `MacroprocessoControllerDesligadoPorDefaultTest`
  (flag no default — rota dá 404), `MacroprocessoToolsTest`,
  `ContextLoadsComFlagDocumentacaoEstruturadaLigadaTest` (contexto completo
  sobe sem erro também com a flag ligada, não só desligada).
- Exclusão de macroprocesso em uso: nenhum código novo necessário — a FK
  `fk_documento_macroprocesso` (sem `ON DELETE`, logo `RESTRICT`) mais o
  `GlobalExceptionHandler` já existente (que já mapeia
  `DataIntegrityViolationException` para 409) cobrem a decisão A4 de
  graça.
- Testes novos: 19 (6 no service — listar, buscarPorId 404, criar,
  atualizar 404, excluir 404, excluir ok; 9 na matriz de segurança do
  controller; 1 confirmando 404 com a flag desligada; 2 na tool; 1 de
  contexto com a flag ligada).
- Suíte completa (execução real): **139/139 passando, 0 skipped, 0 falhas,
  0 erros** (era 120 — aumentou 19, consistente).
- Desvios do plano: nenhum.

### Etapa 5 — Endpoint ADMIN de mudança de `status_ciclo_vida`
- Arquivos: `document/dto/AtualizarStatusCicloVidaRequestDTO.java` (novo);
  `document/service/DocumentoService.java` (`atualizarStatusCicloVida` —
  troca direta, sem fluxo de aprovação, atualiza só `atualizado_por`/`atualizado_em`,
  NÃO versiona conteúdo, NÃO dispara reindexação ainda — isso é da Etapa 17);
  `document/controller/DocumentoStatusCicloVidaController.java` (novo,
  `@ConditionalOnProperty` na flag, controller SEPARADO do `DocumentoController`
  existente — zero linhas tocadas nele); testes novos.
- Testes novos: 5 (service: troca o status, atualiza metadados, não
  versiona nem reindexa; controller: 403 USUARIO/EDITOR, 200 ADMIN com a
  flag ligada; 404 com a flag desligada).
- Suíte completa (execução real): **144/144 passando, 0 skipped, 0 falhas,
  0 erros** (era 139 — aumentou 5, consistente).
- Desvios do plano: nenhum.

### Etapa 6 — Áreas participantes (join table informativa)
- Arquivos: `db/migration/V11__create_documento_area_participante.sql`
  (novo, FK de `documento_id` com `ON DELETE CASCADE`, FK de
  `categoria_id` padrão/`RESTRICT`, `UNIQUE(documento_id, categoria_id)`);
  `document/entity/DocumentoAreaParticipanteEntity.java`,
  `document/repository/IDocumentoAreaParticipanteRepository.java` (novos);
  `categories/service/CategoriaAccessServiceTest.java` (novo — não
  existia nenhum teste direto dessa classe antes; vira a linha de base
  explícita de que o controle de acesso não muda);
  `document/repository/IDocumentoAreaParticipanteRepositoryTest.java` (novo).
- Testes novos: 7 (2 no repositório — salva/encontra por documento,
  `deleteByDocumentoId` remove todos os vínculos; 5 em
  `CategoriaAccessServiceTest` — admin irrestrito, usuário com/sem
  categoria vinculada, `validarAcessoCategoria` lança `AccessDeniedException`,
  e o teste de regressão explícito provando que uma área participante
  não altera o resultado de acesso).
- Suíte completa (execução real): **151/151 passando, 0 skipped, 0 falhas,
  0 erros** (era 144 — aumentou 7, consistente).
- Desvios do plano: nenhum. Nota: a "prova" de que área participante
  não afeta acesso é estrutural, não só comportamental —
  `CategoriaAccessService` nem recebe `IDocumentoAreaParticipanteRepository`
  no construtor, então não há caminho de código algum por onde essa
  tabela poderia influenciar o resultado.

## Verificações V1–V4 (antes da Etapa 7, 2026-10-07)

Todas passaram. V1: só `DocumentoServiceTest.java` foi modificado entre
arquivos de teste pré-existentes, 100% aditivo (confirmado via diff, sem
nenhuma linha `-` de conteúdo de teste). V2: nenhuma migration existente
tocada. V3: migrations V9-V11 aplicadas num banco de dev real com 12
documentos pré-existentes (9 ativos) — todos ficaram com
`tipo_documento='NAO_CLASSIFICADO'`/`status_ciclo_vida='VIGENTE'`, zero
nulos, app/listagem/chat funcionando normalmente. V4: smoke test local
com a flag ligada (CRUD de macroprocesso, 403/409, PATCH de status,
tool `listarMacroprocessos` confirmada funcionando com IA real, fluxo
normal de criação de documento inalterado) — flag desligada e
`compose.yml` revertido ao final, sem nenhum commit dessa etapa de
verificação.

### Etapa 7 — Conteúdo estruturado versionável (jsonb)
- Arquivos: `db/migration/V12__add_conteudo_estruturado.sql` (novo —
  `conteudo_estruturado` jsonb + `versao_schema` nas 3 tabelas);
  `DocumentoEntity`/`DocumentoVersaoEntity`/`RascunhoDocumentoEntity`
  (2 campos novos cada); `DocumentoService` — métodos tocados:
  `criar` (só a chamada a `registrarNovaVersao`, passando `null,null`),
  `atualizar` (passa `null,null` pra `aplicarNovaVersao` — decisão C3,
  invalida estruturado existente no fluxo legado), `restaurarVersao`
  (passa os valores da versão restaurada — traz de volta), `aplicarNovaVersao`
  e `registrarNovaVersao` (2 parâmetros novos cada, quem decide o valor
  é sempre o chamador). `DocumentoTools.confirmarRascunhoPendente`
  **não precisou de nenhuma mudança** — delega pra `atualizar()`/`criar()`,
  que já tratam a regra; confirmado via `git diff` vazio nesse arquivo.
- Testes novos: 5 — (a) fluxo legado sem estruturado mantém o campo
  sempre nulo; (b) atualizar via REST legado invalida estruturado
  existente, versão anterior nunca é tocada (só 1 INSERT novo); (c)
  o mesmo via `confirmarRascunhoPendente` de um rascunho ATUALIZAR
  legado, ponta a ponta com um `DocumentoService` REAL por baixo
  (`DocumentoToolsConfirmarEstruturadoTest`, novo arquivo); (d)
  `restaurarVersao` traz `conteudoEstruturado`/`versaoSchema` de volta
  da versão restaurada; (e) o campo não interfere com nenhuma coluna de
  metadado (independência estrutural).
- Suíte completa (execução real): **156/156 passando, 0 skipped, 0 falhas,
  0 erros** (era 151 — aumentou 5, consistente).
- Desvios do plano: nenhum. Nenhuma asserção de teste existente foi
  alterada (confirmado: diff de `DocumentoServiceTest.java` é
  `88 insertions(+), 0 deletions(-)`).

### Etapa 8 — DTOs estruturados + validação (PROCESSO/PROCEDIMENTO)
- Arquivos: pacote novo `document/dto/estruturado/` com `DocumentoEstruturadoDTO`
  (conteúdo + bloco de metadados C2a) e os records aninhados
  (`EscopoDTO`, `EtapaDTO`, `DecisaoDTO`/`DecisaoOpcaoDTO`, `RegraNegocioDTO`/`TipoRegraNegocio`,
  `ExcecaoDTO`, `RaciEntryDTO`, `SipocDTO`, `GlossarioEntryDTO`,
  `DocumentoRelacionadoDTO`) — todos com `@Description` do langchain4j
  nos campos; decisões/RACI referenciam etapa só por ID (sem estrutura
  recursiva, por causa das limitações do Gemini). `document/service/DocumentoEstruturadoValidator.java`
  (novo) — validação semântica: tipo restrito a PROCESSO/PROCEDIMENTO
  nesta entrega, IDs únicos (etapa/regra/exceção), referências íntegras
  (regra/etapa/decisão/RACI), decisão com ≥2 opções, `proximaEtapaId`
  e `decisao` nunca juntos, nenhuma etapa órfã (alcançabilidade a
  partir da primeira etapa). Nada disto é chamado por nenhuma tool
  ainda — arquivos novos e isolados, zero risco de runtime.
- Testes novos: 16 (9 de Bean Validation — fixture válida sem
  violação, campos obrigatórios rejeitados vazios/nulos, decisão com 1
  opção rejeitada, seções opcionais vazias aceitas, seção opcional nula
  rejeitada; 7 semânticos — fixture válida sem erro, tipo não suportado,
  IDs de etapa duplicados, referência a regra inexistente, decisão com
  1 opção, `proximaEtapaId`+`decisao` ambíguos, etapa órfã, RACI
  referenciando etapa inexistente).
- Suíte completa (execução real): **172/172 passando, 0 skipped, 0 falhas,
  0 erros** (era 156 — aumentou 16, consistente).
- Desvios do plano: nenhum arquivo de teste pré-existente foi tocado
  (só arquivos novos). Correção no caminho: o primeiro `Write` da
  fixture de teste, feito via PowerShell pra tornar métodos do builder
  públicos, introduziu um BOM UTF-8 que quebrou a compilação
  (`illegal character '﻿'`) — corrigido reescrevendo o arquivo
  sem BOM antes de rodar a suíte; nenhum commit chegou a ser feito com
  o BOM.

### Etapa 9 — Renderizador HTML determinístico
- Arquivos: `document/service/EstruturaDocumentoHtmlRenderer.java`
  (novo, standalone — nada o chama ainda); `EstruturaDocumentoHtmlRendererTest.java`
  (novo). Todo texto do LLM é inserido via `Entities.escape` do Jsoup
  antes de montar a tag (nunca HTML cru), e o resultado inteiro ainda
  passa pelo `HtmlSanitizerService` normal ao final. Cada etapa/regra/
  exceção vira um `<h3>` com o ID no próprio texto (ex.: "E01 — Nome"),
  sem precisar mudar `HtmlSectionSplitter` — ele já corta por h1/h2/h3,
  então cada item atômico já nasce como chunk próprio na indexação.
- Testes novos: 3 — ida-e-volta confirmando que `HtmlSectionSplitter`
  gera um chunk por item atômico (etapa/regra); seções opcionais
  ausentes não geram chunk vazio; texto do LLM com tags (`<script>`,
  `<h2>`) nunca vira HTML executável nem seção falsa no splitter.
- Suíte completa (execução real): **175/175 passando, 0 skipped, 0 falhas,
  0 erros** (era 172 — aumentou 3, consistente).
- Desvios do plano: nenhum.

## Pendências P1–P4 (antes da Etapa 10, 2026-10-09)

- **P1**: V1–V3 reexecutadas contra o HEAD atual (agora com V9–V12).
  V1/V2: só `DocumentoServiceTest.java` continua sendo o único teste
  pré-existente tocado (100% aditivo); nenhuma migration antiga
  tocada. V3: banco de dev real (12 documentos, 9 ativos) migrado até
  V12 — os 9 documentos ativos têm `tipo_documento='NAO_CLASSIFICADO'`,
  `status_ciclo_vida='VIGENTE'`, `conteudo_estruturado`/`versao_schema`
  NULL (esperado — nenhum documento tem conteúdo estruturado ainda),
  zero nulos nos campos obrigatórios, app/listagem/chat funcionando.
  `documento_versao`/`documento_rascunho` confirmadas com as colunas
  novas via `\d`. V4 reconfirmada rapidamente (flag ligada → 200 em
  `/macroprocessos`) já que nada nas Etapas 7–9 tocou esse código.
- **P2**: `HtmlSanitizerService` (`Safelist.relaxed()`) já permite
  table/thead/tbody/tr/th/td — confirmado por teste real, não só
  leitura de código. Nenhuma alteração na safelist foi necessária.
  Testes novos: `HtmlSanitizerServiceTest` (2, tabela e lista/strong
  sobrevivem) + um teste novo em `EstruturaDocumentoHtmlRendererTest`
  (RACI com tabela + SIPOC, renderizado e sanitizado, todo o conteúdo
  e a tabela sobrevivem, e o splitter ainda consegue extrair as
  seções depois).
- **P3**: teste novo em `DocumentoServiceTest` — atualizar pelo fluxo
  legado SEM mudança de conteúdo (mesmo hash, só categoria/metadado)
  preserva um `conteudoEstruturado`/`versaoSchema` já existente e não
  cria versão nova (esse branch nunca chama `aplicarNovaVersao`, que é
  quem nulifica).
- **P4**: `build.gradle` não forçava encoding — adicionado
  `tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }`
  + `systemProperty 'file.encoding', 'UTF-8'` na task de teste. Causa
  raiz do incidente da Etapa 8 (BOM) registrada no commit. A partir de
  agora, nenhuma edição de arquivo mais passa por PowerShell
  (`Set-Content`/`Out-File`) — só pelas ferramentas de arquivo
  (Read/Edit/Write), que gravam UTF-8 sem BOM.
- Testes novos (P2+P3): 4. Suíte completa (execução real): **179/179
  passando, 0 skipped, 0 falhas, 0 erros** (era 175 — aumentou 4,
  consistente). Nenhuma asserção de teste existente alterada
  (`DocumentoServiceTest.java`: `31 insertions(+), 0 deletions(-)`).

### Etapa 10 — Investigação: function calling com POJO aninhado (2026-10-08/09) — CONCLUÍDA

Harness descartável (`src/test/java/.../scratchetapa10/Etapa10Investigacao.java`,
**nunca commitado, deletado ao final da investigação**), chamando o
`ChatModel` real do provedor (mesma config de `ChatModelConfig`) via
`AiServices` com uma tool de captura (`registrarDocumentoEstruturado(DocumentoEstruturadoDTO)`)
e o material de exemplo (processo de reembolso) fornecido pelo usuário.

**Incidente de diagnóstico (chave Anthropic)**: as primeiras tentativas
retornaram `401 invalid x-api-key` mesmo após o usuário recriar a chave.
Diagnóstico (sem imprimir valores, só tamanho/prefixo): o `.env` e o
registro `HKCU\Environment` (escopo User do Windows) tinham a chave nova
e correta (`sk-ant-...`); mas o **processo desta sessão** (herdado antes
da correção) continha um `ANTHROPIC_API_KEY` de 31 caracteres, formato
`apikey_...`, que por coincidência bate exatamente com um valor
hardcoded antigo em `.idea/workspace.xml` (run config do IntelliJ) —
provavelmente a origem do placeholder original. Reiniciar o terminal não
reinicia a árvore de processo desta sessão, então a correção nunca
chegou até os comandos executados aqui. **Correção**: o harness passou a
ler as chaves diretamente do `.env` (parse manual: ignora `#`/linha
vazia, remove `\r`, `trim`, remove aspas íguais nas duas pontas — mesma
semântica do Docker Compose), nunca de `System.getenv`. Isso resolveu o
401. Dois incidentes de impressão acidental de valor de chave ocorreram
durante o diagnóstico (um `xxd` com coluna ASCII, um `grep` sem máscara)
e foram reportados ao usuário no momento em que aconteceram.

- **Gemini (`gemini-3.8-flash`, `langchain4j-google-ai-gemini:1.18.0`) —
  4/4 execuções com o DTO aninhado + 1/1 execução com uma tool simples de
  3 parâmetros escalares (mesma assinatura de `prepararCriacaoDocumento`)
  falharam de forma IDÊNTICA**, antes de qualquer avaliação de
  schema/campos ser possível:
  ```
  400 INVALID_ARGUMENT: Function call is missing a thought_signature in
  functionCall parts. This is required for tools to work correctly...
  ```
  Causa: modelos Gemini "thinking" exigem que uma chamada de função
  ecoe um `thought_signature` gerado pelo modelo; o `AiServices` do
  langchain4j 1.18.0 não propaga esse campo. **O teste com a tool simples
  (item 3 pedido pelo usuário) confirma que isso NÃO é um problema do
  DTO aninhado** — é uma quebra genérica de qualquer tool-calling via
  `AiServices` + Gemini nesta versão do langchain4j. **Na prática, isso
  significa que o fluxo de chat ATUAL (produção, tool
  `prepararCriacaoDocumento`) já quebraria hoje se alguém configurasse
  `AI_CHAT_PROVIDER=gemini`** — não é uma regressão desta entrega, é uma
  incompatibilidade pré-existente entre o Gemini "thinking" e
  `langchain4j-google-ai-gemini:1.18.0` que só nunca foi notada porque o
  ambiente de produção sempre usou outro provedor. **Decisão do
  usuário**: Gemini fica fora do escopo de produção (só Anthropic);
  nenhuma correção/atualização de dependência foi tentada por causa
  disso — achado só registrado.

- **Anthropic (`claude-sonnet-5`) — 5/5 execuções concluídas (chave
  corrigida via leitura direta do `.env`), nenhum 401, nenhum erro de
  schema**:
  - **2/5 (execuções 1 e 3) tiveram sucesso completo**: ferramenta
    chamada, DTO aninhado desserializado corretamente, **0 erros** do
    `DocumentoEstruturadoValidator` nas duas. Comparando com o gabarito:
    "Carla" nunca apareceu como responsável (convertida para "Analista
    Financeiro"/"[A DEFINIR]" dependendo da execução); todos os 5 campos
    ausentes do gabarito (limite de almoço, valor de aprovação do
    diretor, critério de data de pagamento, SLA, política formal) foram
    corretamente marcados em `pendencias`, nunca inventados com valor
    concreto; as 5 regras de negócio esperadas (bebida alcoólica, CNPJ
    diferente, nota rasurada, limite de almoço, aprovação do diretor)
    foram capturadas nas duas execuções; a decisão condicional do
    diretor (acima de valor "[A DEFINIR]") foi modelada corretamente nas
    duas. A execução 3 modelou a recusa por CNPJ/rasura como uma decisão
    formal com retorno (além da do gestor) — bate melhor com "pelo menos
    2 decisões com retorno" do gabarito do que a execução 1 (que tratou
    isso como exceção, uma escolha de modelagem defensável, não um erro).
  - **3/5 (execuções 2, 4 e 5) não produziram nada avaliável**: a
    resposta consumiu os 8192 tokens de saída (`maxTokens` configurado,
    igual produção) e terminou com `finishReason=LENGTH` sem completar
    nenhuma chamada de ferramenta nem texto — nem erro, nem documento,
    nem chamada de tool. **Achado novo, relevante para a Etapa 11**: o
    comentário já existente em `ChatModelConfig` sobre `maxTokens(8192)`
    ("dá folga de sobra pro thinking + a resposta") não se confirma para
    este DTO — com um documento estruturado completo, 3 de 5 tentativas
    esgotaram o orçamento antes de produzir qualquer saída utilizável
    (60% de taxa de falha por orçamento de tokens, não por schema/
    alucinação). A Etapa 11 (orçamento de tokens, já prevista no plano)
    precisa necessariamente aumentar esse limite e/ou considerar
    streaming incremental por seção antes de habilitar isso em produção.
  - **Nenhuma alucinação de FATO em campo de conteúdo** nas 2 execuções
    bem-sucedidas (nenhum valor numérico/data/nome de sistema inventado
    fora do material). Metadados sem `@NotNull` (confidencialidade,
    periodicidadeRevisaoMeses, donoProcesso, aprovador) às vezes vieram
    com um valor assumido (ex.: `INTERNO`, `12` meses) em vez de `null`
    — a execução 3 chegou a assumir e AINDA registrar isso em
    `pendencias` ("assumido INTERNO por padrão, não confirmado"). Vale
    considerar, na Etapa 12 (prompt v2), instruir explicitamente a
    deixar metadado não inferível como `null` em vez de assumir default.

**Decisão de escopo resultante (ver seção Flags acima)**: só Anthropic em
produção (`bdocs.documentacao-estruturada.provedores=anthropic`),
provedor padrão do código continua `gemini` (troca é config de deploy),
Etapa 11 medida só com Anthropic, e deve necessariamente revisitar o
`maxTokens` por causa do achado de 60% de esgotamento de orçamento acima.

### Etapa 11 — Investigação: orçamento de tokens (2026-10-09) — CONCLUÍDA

Harness descartável (nunca commitado, deletado ao final), reutilizando o
`ChatModel` real da Anthropic (chave lida só do `.env`, nunca de
variável de ambiente — ver incidente da Etapa 10) com um
`ChatModelListener` medindo, por rodada da conversa: `finishReason`,
tokens de entrada/saída e latência (`System.nanoTime`). Duas
configurações testadas, 5 execuções cada (+ 1 execução bônus da config A
por um erro de repasse de parâmetro do Gradle, descartado sem custo —
só mais um dado válido). `maxTokens=8192` (linha de base) já tinha sido
medido na Etapa 10 (2/5 sucesso completo, 3/5 sem nenhuma saída
utilizável).

- **1. Onde os tokens foram gastos**: **thinking estendido NÃO está
  ativo** na configuração atual (`ChatModelConfig` nunca chama
  `.thinkingType(...)`; builder da Anthropic no langchain4j 1.18.0 só
  ativa thinking se configurado explicitamente) — confirmado por medição
  direta (`aiMessage.thinking()` vazio em 100% das rodadas, Etapas 10 e
  11). **O comentário já existente no código** (`ChatModelConfig`, linhas
  36–41) atribuindo o risco de `maxTokens(8192)` ao "thinking consumir a
  cota" **não corresponde à configuração real** — thinking está
  desligado; quem consome o orçamento inteiro é o próprio JSON do
  documento estruturado (a 1ª rodada da conversa, a que gera a chamada
  da ferramenta). Vale corrigir esse comentário quando o código for
  tocado, para não induzir a equipe a investigar a causa errada no
  futuro.
- **2. Config A (`maxTokens=16384`, prompt igual à Etapa 10), 6
  execuções**: **6/6 produziram um documento** (nenhuma falha total,
  contra 3/5 falhas na linha de base de 8192). Validador limpo em 5/6
  (1 execução teve 3 erros de referência — opções de decisão apontando
  para IDs de exceção em vez de etapa; é exatamente o tipo de erro que a
  decisão A2 prevê corrigir via novo turno, não uma falha do validador).
  Tokens de saída da 1ª rodada: 16384/16384/16079/16339/16384/12782
  (média ≈ 15 725 — 3 das 6 bateram exatamente no teto). **Latência
  total média ≈ 171,5 s** (mín. 120,5 s, máx. 205,6 s) — a 1ª rodada
  isolada já leva 111–150 s. A ferramenta foi chamada 2x (resubmissão
  redundante do mesmo documento) em 3/6 execuções.
- **3. Latência vs. timeouts atuais**: o cliente HTTP da Anthropic no
  langchain4j (`AnthropicClient`, verificado por decompilação) usa
  **readTimeout padrão de 60s** quando `ChatModelConfig` não chama
  `.timeout(...)` (caso atual de produção) — e o frontend
  (`chatService.ts`, `SEND_MESSAGE_TIMEOUT_MS`) usa os **mesmos 60000ms**
  para a rota de envio de mensagem. **Os dois timeouts atuais são
  incompatíveis com a latência medida** (120–206s) — mesmo nas execuções
  bem-sucedidas, a chamada à Anthropic sozinha estouraria o timeout de
  60s do cliente HTTP do langchain4j antes de terminar. Nenhum timeout
  de servidor (Tomcat/Spring) explícito foi encontrado em
  `application.yaml`/`compose.yml` (só o `timeout: 5s` do healthcheck do
  Docker, que é outra coisa).
- **4. Config B (`maxTokens=16384` + instrução de concisão no prompt), 5
  execuções**: **5/5 produziram um documento, 5/5 validador limpo** (0
  erros, melhor que a config A). Tokens de saída da 1ª rodada:
  14485/16149/15027/13149/16384 (média ≈ 15 039 — **4,4% menor** que a
  config A). **Latência total média ≈ 146,1 s** (mín. 128,3 s, máx.
  173,5 s) — **≈15% mais rápida** que a config A. Ferramenta chamada 2x
  em apenas 1/5 (vs. 3/6 na config A). A instrução de concisão (já
  decidida para a Etapa 12) ajuda em todas as métricas medidas, sem
  nenhuma contrapartida observada.
- **5. Correção mínima recomendada** (nenhuma implementada ainda):
  1. Subir `maxTokens` de 8192 para 16384 no bean **compartilhado**
     `ChatModelConfig.chatModel()` (branch Anthropic). Impacto no fluxo
     legado: nenhum esperado — é só o teto, Anthropic cobra e para pelos
     tokens realmente gerados, e os fluxos legados hoje terminam bem
     antes de 8192; só protege contra um caso raro de geração longa
     demais no fluxo legado custar mais antes de parar (risco baixo, já
     seria um sintoma de bug noutro lugar).
  2. Definir `.timeout(Duration.ofSeconds(240))` explícito no builder da
     Anthropic em `ChatModelConfig` (hoje usa o default de 60s/leitura) —
     com margem sobre o pior caso medido (205,6 s).
  3. Subir `SEND_MESSAGE_TIMEOUT_MS` em `chatService.ts` de 60000 para
     algo como 240000 — mesmo padrão já usado hoje (constante dedicada só
     para essa rota, comentário já existente reconhecendo que a rota pode
     "legitimamente levar 10-40s+"; só o valor estava desatualizado para
     este novo fluxo).
  4. Adotar a instrução de concisão (config B) no prompt v2 da Etapa 12 —
     já era uma decisão do usuário; esses números confirmam que ela
     ajuda (tokens, latência e taxa de resubmissão redundante, todas
     melhores).
- **6. Tratamento de `finishReason=LENGTH` sem saída utilizável**
  (proposta, não implementada): quando a resposta da Anthropic terminar
  em `LENGTH` e nem texto nem chamada de ferramenta tiverem sido
  produzidos (caso observado 3x na linha de base de 8192, zero vezes em
  16384 nesta amostra, mas não é garantido que nunca aconteça), a tool
  estruturada (Etapa 13) deveria devolver uma mensagem clara ao usuário
  ("o processo descrito é muito longo/complexo para estruturar de uma
  vez — tente dividir em partes menores") em vez de deixar cair num erro
  genérico ou no timeout do frontend.

**Nenhuma dessas correções foi implementada nesta etapa** (investigação
apenas, nada comitado) — ficam para quando o usuário aprovar a
implementação (provavelmente junto da Etapa 13, que é quem de fato monta
o fluxo de produção).

### Etapa 11b — Reduzir o tamanho da saída (2026-10-09) — CONCLUÍDA, meta NÃO atingida

Usuário não aprovou a correção da Etapa 11 (subir `maxTokens`/timeout) —
motivo: ~15k tokens de saída para um processo PEQUENO deixa <10% de
margem no teto de 16.384 (processos reais maiores estourariam), e a
latência é proporcional aos tokens de saída (a alavanca certa é gerar
MENOS, não esperar mais). Meta definida: **<6.000 tokens de saída e
<60s**, mesma qualidade vs. gabarito. Harness novo, descartável (nunca
commitado, deletado ao final), reaproveitando os `DocumentoEstruturadoDTO`
já capturados na Etapa 11 (sem gastar novas chamadas) para as análises 1–3,
e 10 novas execuções (2 variações × 5) para a análise 4.

- **1. Chamada dupla (por que a tool é chamada 2x em 20–50% das
  execuções)**: a mensagem de retorno original da tool
  ("Documento estruturado registrado com sucesso.") não desencoraja uma
  segunda chamada — o modelo às vezes reinterpreta isso como "ok, pode
  prosseguir/refinar" e chama de novo com o documento revisado. Custo
  medido da chamada redundante (Etapa 11): ~3.700–5.900 tokens de saída
  extra (25–40% acima do custo da 1ª chamada) e ~30–45s de latência
  extra. **Correção testada e CONFIRMADA**: system prompt explícito
  ("você tem APENAS UMA oportunidade de chamar esta ferramenta... chamar
  duas vezes é um erro") + retorno da tool reforçando
  ("NÃO chame esta ferramenta de novo nesta conversa") **eliminou 100%
  das chamadas duplas nas 10 execuções da seção 4** (0/10, contra 3/6 e
  1/5 antes).
- **2. Anatomia do JSON por seção** (estimativa calibrada: tokens reais
  da 1ª rodada ÷ tamanho em caracteres do JSON compacto do documento
  capturado — ratio variou 1.32–1.58 tok/char entre amostras, então são
  proporções direcionais, não uma contagem exata de tokens por campo):
  **`fluxo` (etapas) domina com 39–45% do total** em todas as 3 amostras
  analisadas — de longe a maior seção. Depois: `regrasNegocio` (11–15%),
  `raci` (9–12%), `pendencias` (8–10%), `excecoes` (7–8%), metadados
  (6–8%). `sipoc`, `riscosControles`, `glossario`,
  `sistemasFerramentas`, `indicadores`, `documentosRelacionados`
  somados não passam de ~7%. Tamanho médio de texto livre já é modesto
  (etapa.descricao 86–132 caracteres, ~1 frase) — não há "gordura" óbvia
  de verbosidade por campo; o custo é estrutural (10 campos por etapa ×
  9–11 etapas, cada um repetindo nomes de chave no JSON).
- **3. Redundância do schema** (medido nas 3 amostras, sem alterar o
  validador):
  - **RACI.responsavel é 100% idêntico a etapa.responsavel em 29/29
    linhas verificadas** (3/3 amostras, todas as linhas) — redundância
    total confirmada, zero perda de informação se for derivado da etapa
    em vez de pedido de novo ao modelo. Representa ~26–28% do bloco
    RACI (~370–525 tokens estimados).
  - **SIPOC não é uma derivação limpa** das entradas/saídas das etapas —
    só 0–67% de overlap textual exato entre as 3 amostras (o modelo usa
    frases de nível macro, às vezes com texto ligeiramente diferente do
    das etapas). Não é um candidato seguro para remoção/derivação
    automática sem risco de perda de nuance.
  - Limite de tamanho nas descrições (`@Description` com "máx. 1-2
    frases") e listas macro vazias quando a informação já está nas
    etapas: **testado via instrução de prompt** (ver Variação abaixo),
    não via alteração do schema/validador.
  - Formatação do JSON (indentado vs. compacto): não é controlável pelo
    chamador — a Anthropic decide o formato dos argumentos da tool
    internamente; não investigado mais a fundo (fora do que o
    chamador pode ajustar).
- **4. Duas variações testadas, 5 execuções cada, mesmo material/gabarito,
  `maxTokens=16384` fixo nas duas** (comparação com a Etapa 11, Config B:
  tokens méd. ≈15.039, latência méd. ≈146,1s, chamada dupla 1/5, validador
  limpo 5/5):

  | | Tokens saída (méd.) | Latência total (méd.) | Chamada dupla | Validador limpo |
  |---|---|---|---|---|
  | **Variação 1** — só prompt mais rigoroso (schema IGUAL à produção: instrução de uma única chamada + anti-redundância nas listas macro + conciseness) | **13.897** (−7,6% vs. Config B) | **129,0s** (−11,6%) | **0/5** | 5/5 |
  | **Variação 2** — Variação 1 + schema sem `RaciEntryDTO.responsavel` (derivado da etapa, reconstruído e revalidado com o validador real) | 14.380 (+3,5% vs. Variação 1) | 138,3s (+7,2% vs. Variação 1) | 0/5 | 5/5 (via reconstrução) |

  **Achado inesperado**: remover o campo redundante do schema (Variação
  2) **não melhorou nada — ficou ligeiramente PIOR** que só ajustar o
  prompt (Variação 1), dentro da variância natural entre execuções (a
  variação 2 teve mais dispersão: 12.985–16.181 vs. 13.796–14.010 da
  variação 1). A economia teórica de ~370–525 tokens do campo
  `responsavel` é pequena demais pra se destacar da variância normal de
  execução a execução (~1.000–3.000 tokens), e não justifica a
  complexidade adicional (DTO duplicado, lógica de reconstrução, caminho
  de validação paralelo) **sem nenhum ganho mensurável**. **Variação 1
  (só prompt, zero mudança de schema) é claramente a recomendada.**

- **Meta (<6.000 tokens / <60s) NÃO atingida.** Mesmo a melhor variação
  (13.897 tokens méd., 129s méd.) fica **~2,3x acima da meta de tokens e
  ~2,1x acima da meta de latência**. Causa estrutural: `fluxo` domina o
  custo (39–45%) e já é razoavelmente conciso por item — não há
  redundância fácil de cortar sem perder informação real do processo
  (cada etapa precisa mesmo de id/nome/descrição/responsável/
  entradas/saídas/regras aplicáveis pra ser útil). Reduzir mais exigiria
  ou perder informação, ou uma reestruturação maior (ex.: gerar o
  documento em 2+ chamadas de ferramenta menores — etapas separadas de
  metadados e de fluxo — não testado aqui, mais tokens de entrada
  repetidos por chamada, complexidade de protocolo maior; fica como
  ideia não testada para o futuro, não recomendada agora).

- **5. Timeouts no caminho da requisição** (visibilidade limitada ao que
  está nos dois repositórios):
  - Frontend (`chatService.ts`): rota de envio de mensagem usa
    `SEND_MESSAGE_TIMEOUT_MS=60000`; demais rotas usam o default
    `VITE_API_TIMEOUT` (15000ms).
  - Frontend chama o backend **diretamente** (`VITE_API_BASE_URL`) — o
    `nginx.conf` do frontend só serve os arquivos estáticos da SPA e um
    healthcheck, **não** faz proxy de `/api` (sem `proxy_pass`, sem
    `proxy_read_timeout`) — não é um timeout no caminho desta chamada.
  - Backend: nenhum `server.*`/Tomcat timeout explícito em
    `application.yaml`; cliente HTTP do langchain4j-anthropic usa
    **60s de readTimeout por padrão** (decompilado na Etapa 11,
    `ChatModelConfig` não sobrescreve).
  - **Não visível pelo repositório — preciso que você verifique**: nenhum
    load balancer, proxy reverso, API gateway ou timeout de plataforma
    de hospedagem (Railway/Render/Fly/etc., se for o caso) aparece em
    nenhum dos dois repositórios. Esses, se existirem na infraestrutura
    real de produção, não foram e não puderam ser investigados aqui.

**Alternativa proposta (geração assíncrona), conforme pedido — SEM
implementar**: em vez do chat esperar sincronamente ~130s pela resposta
completa da Anthropic, o backend aceitaria o pedido, devolveria
imediatamente algo como "gerando a proposta de documento estruturado,
isso pode levar alguns minutos..." e dispararia a chamada de IA em segundo
plano (ex.: `@Async`/thread pool dedicado); o rascunho ficaria com um
status "gerando" até a IA terminar, e o usuário seria avisado (polling
periódico do frontend, ou push via WebSocket/SSE) quando a proposta
estivesse pronta para revisão. **Impacto estimado**: elimina o problema
de timeout por completo (não há mais limite de 60s/240s a respeitar
nesse fluxo) e resolve a UX de espera percebida (usuário não fica
olhando uma tela parada por 2+ minutos). Custo: trabalho de backend
(fila/async + nova coluna de status + endpoint ou canal de notificação)
e de frontend (UI de "gerando..." + mecanismo de verificação/notificação)
específicos só para este fluxo — escopo moderado, não trivial, mas bem
menor que uma reestruturação do schema. Precisaria de um teto de
segurança (ex.: 5 min) para não deixar jobs presos indefinidamente, e de
um plano para o caso de falha (reaproveitar o loop de correção da
decisão A2 em segundo plano, sem a presença do usuário no momento).

**Pendência registrada para quando `ChatModelConfig` for tocado (Etapa
13)**: corrigir o comentário sobre `maxTokens(8192)` que atribui o risco
ao "thinking" consumir a cota — thinking está desligado hoje (achado da
Etapa 11), o comentário está desatualizado/incorreto.

### Etapa 13.1 — Migrações aditivas p/ geração assíncrona (2026-10-09)

- `V13__add_geracao_assincrona_rascunho.sql`: `documento_rascunho.conteudo_html`
  passa a nullable (`DROP NOT NULL`); 4 colunas novas
  (`erro_geracao` text, `tentativas_geracao` int not null default 0,
  `instrucoes_adicionais` text, `reservado_em` timestamp); CHECK
  constraint `chk_rascunho_html_obrigatorio_se_pendente_ou_confirmado`
  (R6) garantindo a nível de banco que `conteudo_html` nunca é nulo
  quando `status IN ('PENDENTE','CONFIRMADO')`.
- `StatusRascunho` ganha `GERANDO`/`ERRO_GERACAO` (aditivo — os 3
  valores existentes não mudam de significado).
- `RascunhoDocumentoEntity` ganha os 4 campos novos; `conteudoHtml`
  deixa de ser `nullable=false` no mapeamento JPA (acompanha a coluna).
- `IRascunhoDocumentoRepository`: `findFirstByConversaIdAndStatusInOrderByCriadoEmDesc`
  (R1 — "proposta ativa" agora cobre PENDENTE/GERANDO/ERRO_GERACAO);
  `reservarParaProcessamento` (R3 — UPDATE condicional atômico: só
  reserva se `status=GERANDO`, tentativas < limite, e sem reserva ativa
  ou reserva expirada); `finalizarComSucesso`/`finalizarComErro` (R1 —
  UPDATE condicional que só aplica se `status` ainda for `GERANDO`,
  protegendo contra o usuário ter descartado no meio da geração).
- `IMensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc`
  (R4 — material-fonte do worker é o histórico até o momento da
  solicitação, nunca mensagens de turnos posteriores).
- **Testes novos (12)**: `IRascunhoDocumentoRepositoryGeracaoAssincronaTest`
  — nullable funciona, CHECK constraint rejeita/permite corretamente
  (com `jdbcTemplate` + `EntityManager.clear()` pra evitar cache
  obsoleto do Hibernate ao misturar JDBC puro com JPA na mesma
  transação), reserva atômica só ganha uma vez / falha com tentativas
  esgotadas / falha se não estiver mais GERANDO / reganha após a reserva
  anterior expirar, finalização de sucesso/erro só aplica em GERANDO e
  não sobrescreve um rascunho DESCARTADO no meio da geração (R1),
  "proposta ativa" acha a mais recente entre os estados ativos.
- **Desvio da investigação**: nenhum dos exemplos anteriores do plano
  usava um `conversa_id` aleatório sem uma `ConversaEntity` real — só
  descobri a FK `fk_documento_rascunho_conversa` (pré-existente, V5) ao
  rodar os testes; corrigido criando uma conversa real em cada teste.
- Suíte completa (execução real): **191/191 passando, 0 skipped, 0
  falhas, 0 erros** (era 179 — aumentou 12, consistente com os 12 testes
  novos). Nenhum teste existente alterado.

### Etapa 13.2 — `ChatModel` dedicado ao worker (2026-10-09)

- `ChatModelConfig.chatModel` (chat interativo) agora `@Primary` (R5) —
  nenhuma outra mudança nesse bean; todo o resto do sistema que injeta
  `ChatModel` sem qualificador continua recebendo exatamente este bean.
- Bean novo `chatModelGeracaoEstruturada` (`@ConditionalOnProperty`,
  mesmo padrão de `MacroprocessoTools`) — sempre Anthropic (único
  provedor validado para este fluxo), `maxTokens`/`timeout` lidos de
  `bdocs.documentacao-estruturada.geracao.max-tokens`/`timeout-segundos`
  (default 16384/240, validados na Etapa 11b). **Sem**
  `logRequests`/`logResponses` — evita vazar o conteúdo do documento nos
  logs (R3: o worker vai logar só tokens/latência, na Etapa 13.4).
- `application.yaml`: bloco `bdocs.documentacao-estruturada.geracao.*`
  novo (max-tokens, timeout-segundos, max-tentativas=2, 
  timeout-gerando-minutos=5, intervalo-job-ms=60000,
  material-max-caracteres=60000). **Correção de default**: `provedores`
  passa de `gemini,anthropic` para `anthropic` — alinha o código à
  decisão já registrada no plano (Etapa 10/11b); seguro porque a flag
  geral segue `false` por default e a feature nunca esteve live.
- **Pendência R5 corrigida nesta etapa**: comentário sobre
  `maxTokens(8192)`/thinking em `ChatModelConfig` reescrito (thinking
  está desligado, não é a causa do risco de esgotamento).
- Testes novos (2): `ChatModelConfigPrimarioTest` — com a flag ligada,
  injeção de `ChatModel` sem qualificador resolve pro bean primário sem
  ambiguidade (prova em tempo de contexto, não só em asserção: se
  `@Primary` estivesse errado, o contexto nem subiria), e o bean do
  worker é uma instância diferente. O caminho com a flag desligada já é
  coberto pelo `BusinessDocsAiApplicationTests` existente (só 1 bean de
  `ChatModel` continua existindo, igual antes).
- Suíte completa (execução real): **193/193 passando, 0 skipped, 0
  falhas, 0 erros** (era 191 — aumentou 2, consistente). Nenhum teste
  existente alterado.

### Etapa 13.3 — Tool leve + R1 nas tools legadas + confirmação estruturada (2026-10-09)

Esta etapa acabou incorporando, além da tool leve planejada, a lógica de
confirmação que separa conteúdo de metadado (decisão B3) — ela só pode
viver em `DocumentoTools.confirmarRascunhoPendente` (não existe endpoint
REST de confirmação, só o caminho do chat), então faz mais sentido no
mesmo commit que toca esse método por causa do R1.

- **`StatusRascunho.ativos()`**: método estático novo, substitui a
  constante local que eu ia duplicar em `DocumentoTools` e
  `DocumentoEstruturadoTools` — fonte única da regra "quais estados
  contam como proposta ativa de uma conversa" (R1).
- **`DocumentoEstruturadoTools`** (novo, `@ConditionalOnProperty` como
  `MacroprocessoTools`): `solicitarGeracaoDocumentoEstruturado`/
  `solicitarAtualizacaoDocumentoEstruturado` — SÓ recebem título/
  categoria/ID-alvo/instruções opcionais, NUNCA o documento completo.
  Criam o rascunho em `GERANDO` e publicam `GeracaoEstruturadaSolicitadaEvent(rascunhoId)`.
  Material-fonte NÃO viaja como argumento da tool (evita o modelo
  resumir/perder fidelidade) — o worker (Etapa 13.4) vai ler do
  histórico persistido (R4).
- **R1 aplicado nas 4 tools legadas de `DocumentoTools`**:
  `prepararCriacaoDocumento`/`prepararAtualizacaoDocumento` agora
  recusam com mensagem clara se já há um rascunho em `GERANDO` na
  conversa (e colapsam em cima de um `PENDENTE`/`ERRO_GERACAO`
  existente, igual sempre fez); `confirmarRascunhoPendente` recusa
  `GERANDO` ("ainda está sendo gerado") e `ERRO_GERACAO` (mostra a
  mensagem de erro); `descartarRascunhoPendente` passou a aceitar
  descartar um rascunho em `GERANDO` também. **Nenhuma regra nova
  inventada** — é a mesma regra de "só uma proposta ativa por
  conversa" que já existia pra `PENDENTE`, agora olhando os 3 estados
  ativos.
- **`DocumentoEstruturadoAplicadorService`** (novo): na confirmação, se
  `rascunho.conteudoEstruturado != null`, desserializa o JSON completo
  (conteúdo+metadado juntos — assim o rascunho sempre guardou, ver
  comentário de Etapa 7), separa em (a) JSON só de conteúdo e (b)
  `DocumentoEstruturadoMetadadosDTO`, e chama os métodos novos de
  `DocumentoService`. `processoPaiId` inválido (não-UUID) vindo do
  modelo é ignorado, não quebra a confirmação.
- **`DocumentoService.criarComEstrutura`/`atualizarComEstrutura`**
  (novos — `criar`/`atualizar` existentes INTOCADOS): aplicam o bloco
  de metadados nas colunas do documento (`tipoDocumento`,
  `macroprocessoId`, `processoPaiId` com `validarHierarquiaProcesso`,
  `donoProcesso`, `aprovador`, `periodicidadeRevisaoMeses`,
  `confidencialidade`, `tags`) e substituem as áreas participantes
  (`DocumentoAreaParticipanteEntity`). `atualizarComEstrutura` sempre
  versiona (nunca pula por hash igual — confirmar uma proposta
  estruturada é sempre deliberado, diferente de uma edição de rotina).
- **`RagAssistantConfig`**: `DocumentoEstruturadoTools` só é adicionada
  à lista de tools do assistente quando
  `FeatureFlags.documentacaoEstruturadaHabilitadaPara(provedorAtual)` —
  gate adicional além do bean existir, pra nem oferecer a tool leve a
  um provedor onde o tool-calling já quebra (Gemini).
- **Testes existentes tocados (mecânico, zero asserção alterada)**:
  `DocumentoServiceTest.java` (+1 mock, +1 import, 1 linha de
  construtor estendida — `6 insertions(+), 1 deletion(-)` só pela linha
  do construtor crescer), `DocumentoToolsTest.java` e
  `DocumentoToolsConfirmarEstruturadoTest.java` (novo mock/import +
  nome do método de repositório stubado atualizado pra bater com o que
  o código agora chama — nenhuma asserção removida/alterada, só
  acrescentadas).
- **Testes novos (21)**: `DocumentoEstruturadoToolsTest` (7 — cria
  `GERANDO`, publica evento, recusa sem acesso, recusa com outro
  `GERANDO` ativo, reaproveita `ERRO_GERACAO` preservando
  `turnoCriacao`, atualização com ID inválido/sem acesso/sucesso);
  `DocumentoEstruturadoToolsDesligadoPorDefaultTest` +
  `...LigadoComAFlagTest` (2 — existência do bean por flag);
  `DocumentoEstruturadoAplicadorServiceTest` (3 — separação
  conteúdo/metadado pra CRIAR e ATUALIZAR, `processoPaiId` inválido
  ignorado); `DocumentoServiceComEstruturaTest` (4 — metadados
  aplicados, `NAO_CLASSIFICADO` sem metadados, sempre versiona,
  autorreferência de hierarquia rejeitada); 5 novos métodos em
  `DocumentoToolsTest` (R1: recusa com `GERANDO` ativo, confirmar
  recusa `GERANDO`/`ERRO_GERACAO`, descartar funciona em `GERANDO`,
  confirmar com estruturado delega pro aplicador).
- Suíte completa (execução real): **214/214 passando, 0 skipped, 0
  falhas, 0 erros** (era 193 — aumentou 21, consistente com os 21
  testes novos). Nenhum teste existente teve asserção alterada.

**Desvio encontrado e corrigido — CI vermelho na Etapa 13.2**: o push da
13.2 passou localmente (193/193) mas o CI falhou. Causa raiz:
`AnthropicChatModel.builder()` valida eagerly que `apiKey` não é
branco; o CI não define `ANTHROPIC_API_KEY` nenhuma (só as chaves
fictícias de JWT), então `app.ai.anthropic-api-key` resolve pra string
vazia em CI — e meu bean novo (`chatModelGeracaoEstruturada`) é
construído sempre que a flag está ligada, sem depender de o provedor
ser de fato Anthropic. Isso quebrou o teste JÁ EXISTENTE
`ContextLoadsComFlagDocumentacaoEstruturadaLigadaTest` (da Etapa 4) e
teria quebrado os 2 testes novos flag-ligada desta sessão
(`ChatModelConfigPrimarioTest`, `DocumentoEstruturadoToolsLigadoComAFlagTest`).
Passou despercebido localmente só porque o shell desta sessão tinha uma
`ANTHROPIC_API_KEY` real (herdada do trabalho da Etapa 10/11),
mascarando o problema. **Correção**: os 3 testes com a flag ligada
passam a fixar `app.ai.anthropic-api-key` com uma chave FAKE (só não-
branca, nunca chama a API de verdade neste contexto de teste) via
`@TestPropertySource`. Revalidado localmente com `ANTHROPIC_API_KEY`
explicitamente removida do ambiente (replicando as condições do CI) —
214/214 confirmado sem nenhuma chave real presente.

### Etapa 13.4 — Worker de geração (2026-10-09)

- **`GeracaoEstruturadaSystemPrompt`**: texto "V1" validado na Etapa
  11b (única chamada + anti-redundância + concisão), independente do
  `RagSystemPrompt` do chat interativo.
- **`GeracaoEstruturadaService`** (`@ConditionalOnProperty`, mesmo
  padrão das demais peças): `processar(rascunhoId)` — reserva atômica
  (R3), monta o material (R4: histórico persistido até o momento da
  solicitação + anexos/áudio já extraídos em `MensagemEntity.conteudo`
  + instruções adicionais), checa o limite de tamanho (R4 — vira
  `ERRO_GERACAO` com mensagem clara, sem nem chamar a IA), chama a IA
  com um laço de correção interno (decisão A2 adaptada: até 2
  correções por tentativa, na MESMA `ChatMemory` efêmera — nunca
  persistida), valida (Bean Validation + `DocumentoEstruturadoValidator`),
  renderiza o HTML e finaliza com sucesso ou erro (ambos via UPDATE
  condicional — R1). Exceção na chamada de IA ou documento inválido
  após as correções: vira `ERRO_GERACAO` SÓ se `tentativasGeracao` (R3,
  reservas entre o listener e o job) já esgotou o limite; senão, fica
  em `GERANDO` pro job de segurança tentar de novo.
- **`gerarDocumentoValidado` extraído como método `protected`** de
  propósito — permite testar a ORQUESTRAÇÃO (reserva/limite de
  material/decisão retentar-vs-desistir/finalização) com um `spy`
  substituindo só essa chamada, sem precisar de um `ChatModel` fake
  reproduzindo o protocolo inteiro de tool-calling do langchain4j (a
  integração de verdade com esse exato prompt/schema já foi validada
  empiricamente contra a API real nas Etapas 10/11/11b).
- **`ChatModelConfig.chatModelGeracaoEstruturada`** ganhou um
  `ChatModelListener` (R3) que loga `finishReason`/tokens de entrada e
  saída/latência por chamada — NUNCA o conteúdo — usando `MDC` pra
  correlacionar os logs de uma geração pelo `rascunhoId` (setado por
  `GeracaoEstruturadaService.processar` antes de chamar a IA).
- **`GeracaoEstruturadaListener`**/**`GeracaoEstruturadaJob`**: mesmo
  par de padrões já usado pra indexação (`IndexacaoListener` +
  `ReindexacaoJob`) — reação imediata via
  `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` num executor
  dedicado (`geracaoEstruturadaExecutor`, `AsyncConfig`), e um
  `@Scheduled` como rede de segurança pra rascunhos presos em `GERANDO`
  (evento perdido ou processo reiniciado no meio). A reserva atômica
  (R3) garante que os dois nunca processam o mesmo rascunho ao mesmo
  tempo.
- **Testes novos (19)**: `GeracaoEstruturadaServiceTest` (14 —
  orquestração via `spy`: reserva falha, material grande, sucesso,
  resultado descartado em silêncio quando não está mais `GERANDO`,
  inválido com/sem tentativas esgotadas, exceção com/sem tentativas
  esgotadas; validação Bean+semântica sem IA; `FerramentaCaptura`
  isolada; `montarMaterial` com/sem instruções adicionais);
  `GeracaoEstruturadaListenerTest` (1); `GeracaoEstruturadaJobTest`
  (2); `GeracaoEstruturadaBeansDesligadoPorDefaultTest` (1) +
  `...LigadoComAFlagTest` (1) — existência de todas as peças novas por
  flag, incluindo o `chatModelGeracaoEstruturada`.
- Suíte completa (execução real, **sem nenhuma `ANTHROPIC_API_KEY` no
  ambiente**, replicando o CI): **233/233 passando, 0 skipped, 0
  falhas, 0 erros** (era 214 — aumentou 19, consistente). Nenhum teste
  existente alterado.

### Etapa 13.5 — Endpoint de leitura ampliado (2026-10-09)

A regra "confirmação só em PENDENTE" já tinha sido implementada na
Etapa 13.3 (junto do resto de R1, já que `confirmarRascunhoPendente` é
o único caminho de confirmação — não existe endpoint REST separado).
Esta etapa cobre só o que faltava: o endpoint de LEITURA.

- **`PropostaDocumentoDTO`** ganha `status`/`erroGeracao` (aditivo —
  único call site de construção, sem necessidade de construtor de
  compatibilidade). `"PENDENTE"` continua sendo o único valor possível
  pro fluxo legado, exatamente como sempre foi.
- **`ChatService.buscarPropostaDocumentoPendente`** passa a procurar
  `StatusRascunho.ativos()` (PENDENTE/GERANDO/ERRO_GERACAO) em vez de
  só PENDENTE — tanto o `GET /chat/conversas/{id}/rascunho-pendente`
  quanto a resposta de `enviarMensagem` passam a mostrar o estado
  "gerando..."/erro, não só a proposta pronta (R1).
- **Teste existente tocado (mecânico)**: `ChatServiceTest.java` — 2
  stubs do método antigo (`findFirstByConversaIdAndStatusOrderByCriadoEmDesc`,
  só PENDENTE) atualizados pro método novo
  (`findFirstByConversaIdAndStatusInOrderByCriadoEmDesc`, `ativos()`)
  que o código agora chama de fato — sem isso, os 2 testes existentes
  que afirmam `propostaDocumento()` não-nulo/nulo quebrariam
  silenciosamente (mock sem stub correspondente devolve
  `Optional.empty()` por padrão). Nenhuma asserção removida/alterada,
  só os stubs renomeados pra bater com a chamada real.
- **Testes novos (2)**: proposta com `status=GERANDO` e
  `conteudoHtml=null` enquanto a geração está em andamento; proposta
  com `status=ERRO_GERACAO` e a mensagem de erro visível.
- Suíte completa (execução real, sem `ANTHROPIC_API_KEY`): **235/235
  passando, 0 skipped, 0 falhas, 0 erros** (era 233 — aumentou 2,
  consistente).

### Etapa 13.6 — Frontend: polling no painel existente (2026-10-09)

Repositório `businessDocsAi-frontend` (sem framework de teste automatizado
configurado no projeto — validação por `tsc -b` + `eslint`, ambos limpos,
e teste manual no browser, ver relatório final).

- **`types/index.ts`**: `PropostaDocumento` ganha `status`
  (`'PENDENTE'|'GERANDO'|'ERRO_GERACAO'`) e `erroGeracao` (aditivo);
  `conteudoHtml` passa a `string | null` (é `null` enquanto
  `status==='GERANDO'`).
- **`DocumentPanel.tsx`**: o mesmo painel `documentDraft` passa a
  renderizar 3 estados — `GERANDO` (spinner + mensagem), `ERRO_GERACAO`
  (mensagem de erro + sugestão de regenerar/descartar), e o conteúdo
  normal pra `PENDENTE` (inalterado).
- **`useChat.ts`**: novo polling (R7) — enquanto o painel central é uma
  proposta com `status==='GERANDO'`, confere
  `chatService.getPendingDraft` (endpoint já existente, nenhum novo) a
  cada 4s, só troca o painel quando o status mudar E ainda for a MESMA
  proposta (`rascunhoId` igual — evita aplicar uma proposta diferente
  por engano se o usuário trocou de ideia nesse meio tempo). Para
  sozinho ao: status mudar, trocar de conversa/proposta, desmontar o
  componente, ou passar de um teto de 5 minutos. Sem WebSocket/SSE.
  Com a flag desligada no backend, `status` nunca é `'GERANDO'` —
  nenhum polling novo chega a começar.

#### Bug 1 (produção, achado após o redeploy) — falta de `@Transactional` nos métodos do worker

Teste manual de ponta a ponta com a flag ligada: rascunho ficou preso em
`GERANDO` indefinidamente. Diagnóstico (DB + logs + análise da própria
espera, pedido explicitamente pelo usuário antes de corrigir):
`reservarParaProcessamento`/`finalizarComSucesso`/`finalizarComErro`
(`@Modifying(flushAutomatically=true)`) exigem uma transação ativa;
nem `GeracaoEstruturadaListener` (`@Async`) nem `GeracaoEstruturadaJob`
(`@Scheduled`) nem `GeracaoEstruturadaService.processar()` tinham
`@Transactional` — falhava em produção com `InvalidDataAccessApiUsageException`
("No EntityManager with actual transaction available"), mascarado nos
testes porque `IRascunhoDocumentoRepositoryGeracaoAssincronaTest` é
`@Transactional` na própria classe (fornece uma transação "de graça").

- **Correção**: `@Transactional` direto nos 3 métodos de
  `IRascunhoDocumentoRepository` (garante transação própria
  independente de quem chama).
- **Teste novo**: `IRascunhoDocumentoRepositorySemTransacaoAmbienteTest`
  (deliberadamente SEM `@Transactional` na classe) — 3 testes,
  reproduz e confirma a correção.
- Também corrigidos nessa mesma rodada (achados ao investigar por que a
  geração real, já com o fix acima, ainda timeoutava):
  - `GeracaoEstruturadaService` usava `@RequiredArgsConstructor` do
    Lombok, que **não copia `@Qualifier` do campo pro parâmetro do
    construtor gerado** (confirmado via `javap`) — a resolução do
    `ChatModel` dedicado dependia só do fallback do Spring por nome
    (funciona, mas quebra em silêncio pro `@Primary` se o nome um dia
    divergir). Trocado por construtor explícito com `@Qualifier` no
    parâmetro. Teste novo: `GeracaoEstruturadaBeansLigadoComAFlagTest`
    passa a provar (via `ReflectionTestUtils`) que o worker recebe de
    fato o bean dedicado, não o `@Primary`.
  - `maxRetries` do `AnthropicChatModel` do worker nunca era setado —
    default do langchain4j é 2 (confirmado via `javap`), multiplicando
    silenciosamente o custo/latência de CADA tentativa do R3 (que já
    tem seu próprio mecanismo de retry). Novo
    `maxRetries(maxRetriesHttp)`, configurável via
    `geracao.max-retries-http` (default 0 — só o R3 decide se tenta de
    novo). Log de inicialização novo com a config efetiva (modelo,
    maxTokens, timeout, maxRetriesHttp — nunca a chave).

#### Bug 2 — `criarComEstrutura` não espelhava `conteudo_estruturado`/`versao_schema` no `documento`

Achado no teste manual E2E (modo `modelo-fake`, ver abaixo): depois de
confirmar uma proposta estruturada NOVA (criação, não atualização), a
coluna `documento.conteudo_estruturado` ficava `NULL` — só
`documento_versao` tinha o valor. `atualizarComEstrutura`/
`restaurarVersao` (via `aplicarNovaVersao`) sempre espelharam esses 2
campos na linha `documento`; `criarComEstrutura` chamava
`registrarNovaVersao` direto (só grava a versão), pulando esse espelho.

- **Correção**: `criarComEstrutura` agora seta
  `documento.setConteudoEstruturado(...)`/`setVersaoSchema(...)` antes
  de salvar, mesma regra de `aplicarNovaVersao`.
- **Teste**: `DocumentoServiceComEstruturaTest.criarComEstruturaAplicaMetadadosNoDocumentoEGravaConteudoEstruturadoNaVersao`
  ganhou as 2 asserções que faltavam (o teste existente só conferia a
  versão, nunca o `documento` — por isso o bug passou despercebido por
  243 testes).

#### Bug 3 — `descartarRascunhoPendente()` podia apagar um resultado que o worker tinha acabado de commitar

Achado no teste manual E2E (descartar durante a geração, modo
`modelo-fake`): o tool fazia `findFirst...()` (lê a entidade) → muda só
`status` em memória → `save()` da entidade INTEIRA. Se o worker
terminasse (`finalizarComSucesso`, UPDATE condicional em
`status='GERANDO'`) ENTRE essa leitura e esse `save()`, o `save()` do
descarte reescrevia TODAS as colunas com os valores ANTIGOS da leitura
— apagando `titulo`/`conteudo_html`/`conteudo_estruturado` que o worker
tinha acabado de gravar. Não era "deixar de reviver" (o que R1 já
cobria) — era perder dado já commitado.

- **Correção**: novo método `IRascunhoDocumentoRepository.descartar(id,
  statusAtivos)` — UPDATE condicional só na coluna `status` (mesmo
  padrão de `reservarParaProcessamento`/`finalizarComSucesso`/
  `finalizarComErro`), nunca toca nenhuma outra coluna.
  `descartarRascunhoPendente()` trocado para usar esse método em vez de
  `save()` da entidade inteira.
- **Testes**: `IRascunhoDocumentoRepositoryGeracaoAssincronaTest` ganhou
  2 testes novos —
  `saveDeEntidadeDesatualizadaSobrescreveConteudoCommitadoPeloWorker_antiPadrao`
  (documenta deliberadamente o comportamento perigoso do `save()`
  antigo — prova de por que a troca era necessária) e
  `descartarCondicionalNaoApagaConteudoJaCommitadoPeloWorker` (prova
  que o método novo não sofre do mesmo problema). `DocumentoToolsTest`
  — os 2 testes existentes de descarte atualizados pro novo método
  condicional (não mais `save()`), e 1 teste novo pro caso
  `descartados=0` (worker terminou ou já foi descartado nesse
  meio-tempo).
- Suíte completa (execução real, sem `ANTHROPIC_API_KEY`, igual ao CI):
  **246/246 passando, 0 skipped, 0 falhas, 0 erros**.

#### Modo `modelo-fake` — teste manual sem gastar créditos da API real

Regra de custo imposta em 2026-10-10 (ver seção no topo do arquivo) após
2 recargas da Anthropic em 1 dia. Novo
`bdocs.documentacao-estruturada.geracao.modelo-fake` (default `false`,
NUNCA ligado em produção): com `true`, `GeracaoEstruturadaService` NUNCA
chama `chatModelGeracaoEstruturada` — devolve um documento de exemplo
fixo e válido (processo de reembolso, 2 etapas, 1 regra, RACI, Bean +
semanticamente válido) depois de um atraso curto e configurável
(`modelo-fake-atraso-ms`, default 5000ms — só pra UI ter o estado
GERANDO pra mostrar). Usado pra todo o teste manual de ponta a ponta
(A, abaixo) — Parte B (1 geração real) é a única chamada que de fato
usa a Anthropic.

**Postura de produção (F3, 2026-10-10)**: `.env` LOCAL fica com
`DOCUMENTACAO_ESTRUTURADA_ENABLED=true` e `..._MODELO_FAKE=true`
enquanto o desenvolvimento das Etapas 14+ continua (nenhuma chamada real
à Anthropic nesse ambiente a partir de agora, a menos que explicitamente
reautorizado). **Em produção, `DOCUMENTACAO_ESTRUTURADA_ENABLED` fica
DESLIGADA até o lançamento da feature** — e `modelo-fake` nunca deve ser
ligado em produção em hipótese nenhuma (o código já trata isso como
default `false`, mas a variável de ambiente de produção nunca deve nem
declarar `true`).

#### F2 — re-pedir depois de ERRO_GERACAO (2026-10-10)

Comportamento ANTERIOR (Etapa 13.3): `DocumentoEstruturadoTools.solicitar()`
reaproveitava em memória um rascunho em ERRO_GERACAO (mesma regra de
PENDENTE) — resetava status/tentativas/erro na MESMA linha, nunca
passando por DESCARTADO. Funcionava (nunca exigia descarte manual), mas
apagava o histórico do erro anterior e herdava `reservadoEm` de uma
geração completamente diferente.

**Corrigido**: ERRO_GERACAO nunca mais é reaproveitado em memória — é
descartado via `repository.descartar()` (UPDATE condicional, mesmo
método do bug 3 da Etapa 13.6) e uma linha NOVA é criada do zero
(tentativas=0, turnoCriacao = turno ATUAL, não o antigo). PENDENTE
continua reaproveitado em memória, inalterado (regra legada).

**Tools legadas (`DocumentoTools`) confirmadas seguras** diante de um
rascunho em GERANDO/ERRO_GERACAO, com teste para cada uma: `preparar*`
recebem o HTML completo como parâmetro e SOBRESCREVEM a linha — nunca
leem/mesclam o `conteudo_html` antigo (que pode estar nulo, já que o
worker assíncrono pode falhar antes de escrever nada); `confirmarRascunhoPendente`
recusa explicitamente para os dois estados antes de ler qualquer
conteúdo; `descartarRascunhoPendente` não toca em nenhuma coluna de
conteúdo. Único gap de cobertura encontrado: faltava teste de
`prepararAtualizacaoDocumento` recusando com GERANDO — adicionado.

Testes novos: 5 (`DocumentoEstruturadoToolsTest`: split do teste antigo
em 2 — reaproveita PENDENTE / descarta+cria novo em ERRO_GERACAO;
`DocumentoToolsTest`: GERANDO em `prepararAtualizacaoDocumento`, +
ERRO_GERACAO sobrescreve sem ler o antigo em `prepararCriacaoDocumento`
e `prepararAtualizacaoDocumento`).

### Etapa 14 — Metadados novos no chunk do RAG (2026-10-10)

- **Migration `V14__add_metadados_rag_documento_embedding.sql`**:
  `ALTER TABLE IF EXISTS documento_embedding ADD COLUMN IF NOT EXISTS`
  para `categoria_id`/`tipo_documento`/`status_ciclo_vida`/
  `macroprocesso_id`/`confidencialidade` (todas nullable) — cobre o
  banco EXISTENTE (B1).
- **`EmbeddingConfig`**: `columnDefinitions` do `PgVectorEmbeddingStore`
  ganhou as mesmas 5 colunas — cobre um banco NOVO (`createTable(true)`
  já cria com elas desde o início; a migration vira no-op nesse caso).
- **`IndexacaoService.gerarSegmentos`**: grava os 5 metadados no chunk.
  `categoria_id` sempre (NOT NULL em `documento`, V1); os outros 4 só
  quando presentes — `NAO_CLASSIFICADO` tratado como "sem tipo" (omitido
  do metadado E do prefixo do texto, não só um valor nulo). (A1) tipo
  complementa o prefixo título+seção que já existia.
- **B1 — os dois cenários testados**: banco EXISTENTE (test DB local,
  já tinha a tabela com o schema antigo de execuções anteriores) — rodar
  a suíte local aplicou a migration V14 de verdade via Flyway, 5 colunas
  novas confirmadas via `\d documento_embedding` sem perder nenhuma
  coluna/dado existente. Banco NOVO (`createTable`) — coberto
  naturalmente pelo CI, que sobe um Postgres efêmero do zero a cada
  execução (nunca viu a tabela antes).
- Testes novos: 2 (`IndexacaoServiceTest` — grava os 5 metadados quando
  presentes; omite os 4 opcionais, e o tipo do prefixo do texto, quando
  o documento é `NAO_CLASSIFICADO`/sem metadado). Fixture `documentoComVersao`
  corrigido para sempre setar `categoriaId` (é NOT NULL no banco de
  verdade — o fixture antigo não precisava disso antes da Etapa 14).
- Suíte completa: 252/252 passando, 0 falhas, 0 erros (era 250).

### Etapa 15 — Reindexação em massa, ADMIN/manual/assíncrona (2026-10-10)

Checagem pré-etapa (regra de custo): `AI_EMBEDDING_PROVIDER=local`
(ONNX em processo, sem chave/sem chamada de rede) — reindexação em
massa local autorizada, zero custo de API real.

- **`ReindexacaoEmMassaService`** (`ai/ingestion`): `reindexarTodos()`
  (`@Async("reindexacaoEmMassaExecutor")`) varre
  `findByDeletadoFalseOrderByTituloAsc()` e chama
  `IndexacaoService.indexar(id, versaoAtual)` documento por documento —
  reaproveita toda a lógica existente (remove chunks antigos, gera
  novos, confere versão vigente), nada duplicado. `contarDocumentosAtivos()`
  pra resposta imediata do endpoint.
- **`AsyncConfig`**: novo `reindexacaoEmMassaExecutor`, pool PRÓPRIO e
  pequeno (core=1/max=1) — nunca o `indexacaoExecutor` compartilhado,
  pra uma reindexação de centenas de documentos não entupir a fila da
  indexação normal (criar/editar documento) atrás de si.
- **`ReindexacaoEmMassaController`**: `POST /admin/reindexacao`,
  `hasRole('ADMIN')`, `@ConditionalOnProperty` da flag (mesmo padrão de
  `DocumentoStatusCicloVidaController`) — devolve 202 com a contagem de
  documentos no momento do disparo; o trabalho em si roda em segundo
  plano.
- Testes novos: 7 (`ReindexacaoEmMassaServiceTest`: reindexa cada
  documento com sua própria versão vigente, lista vazia não chama
  indexação, contagem; `ReindexacaoEmMassaControllerSecurityTest`:
  USUARIO/EDITOR 403, ADMIN 202; `ReindexacaoEmMassaControllerDesligadoPorDefaultTest`:
  rota não existe com a flag desligada).
- Suíte completa: 259/259 passando, 0 falhas, 0 erros (era 252).

### Etapa 16 — Filtro pré-busca no RAG (2026-10-10)

**B5 resolvido** — confirmado via bytecode do `langchain4j-core` 1.18.0
(`filter/comparison/` + `filter/logical/`): só existem `IsEqualTo`/
`IsNotEqualTo`/`IsGreaterThan(OrEqualTo)`/`IsLessThan(OrEqualTo)`/`IsIn`/
`IsNotIn`/`ContainsString` + `And`/`Or`/`Not`. **Sem `IsNull`/`IsNotNull`**
— não dá pra expressar "`status_ciclo_vida = 'VIGENTE' OR status_ciclo_vida
IS NULL`" diretamente. Decisão do usuário (parou antes de codar, como
pedido): valor sempre gravado + proteção automática (não a alternativa
de só manter o filtro pós-busca).

- **Valor sempre gravado (requisito 1)**: `IndexacaoService` agora grava
  `status_ciclo_vida` SEMPRE (nunca omite a chave) — `VIGENTE` quando o
  documento não tem status real (`DocumentoEntity.isVigente()`, novo —
  `null` conta como `VIGENTE`, "vigente por padrão", decisão 7/C4).
  `categoria_id` já era sempre gravado desde a Etapa 14 (`NOT NULL` em
  `documento`). Os outros 3 metadados (tipo/macroprocesso/confidencialidade)
  continuam opcionais — não entram no pré-filtro.
- **Proteção automática (requisito 2)** — `RagCicloVidaFiltroService`
  (`ai/retrieval`, novo): antes de aplicar o pré-filtro, verifica (SQL
  direto, `SELECT count(*) FROM documento_embedding WHERE status_ciclo_vida
  IS NULL OR categoria_id IS NULL`, cache em memória com TTL configurável
  — `bdocs.rag.filtros-ciclo-vida.cache-ttl-minutos`, default 5min) se
  existe QUALQUER chunk órfão (indexado antes da Etapa 14). Se existir:
  pré-filtro NÃO aplicado (busca funciona como hoje) + `WARN` no log com
  a contagem e a instrução de rodar `POST /admin/reindexacao`. Se não
  existir E a flag `bdocs.rag.filtros-ciclo-vida.enabled` (novo, default
  `false`) estiver ligada: pré-filtro aplicado
  (`IsEqualTo("status_ciclo_vida", "VIGENTE")`). Usado nos dois lugares
  (requisito obrigatório): `PesquisaService` (`EmbeddingSearchRequest.filter`)
  e `RagAssistantConfig` (`EmbeddingStoreContentRetriever.dynamicFilter`
  — não `filter()` estático, porque a decisão depende do estado ATUAL do
  banco, reavaliada a cada busca).
- **Filtro pós-busca confere o BANCO, não o chunk (requisito 3)**:
  `PesquisaService.documentoAcessivel` e
  `RagAssistantConfig.acessivelPelaCategoria` ganham
  `.filter(DocumentoEntity::isVigente)` — a mesma regra "vigente por
  padrão", mas lida da entidade `documento` (fonte da verdade), nunca do
  metadado do chunk (que pode estar desatualizado/incompleto).
- **Visibilidade ADMIN (requisito opcional 4, aceito)**: `GET
  /admin/reindexacao/status` devolve `chunksSemMetadado` e
  `preFiltroCicloVidaAtivo` — pra saber quando a reindexação em massa já
  corrigiu todos os chunks órfãos (sujeito ao mesmo cache/TTL).
- **Teste mandatório** (resultados idênticos com a flag ligada/desligada
  quando há chunk legado sem metadado) — provado em 2 camadas:
  `RagCicloVidaFiltroServiceTest.comMetadadosIncompletosNuncaAplicaOPreFiltroIndependenteDaFlag`
  (o serviço devolve `Optional.empty()` nos dois estados da flag quando
  a contagem de chunks órfãos é > 0); `PesquisaServiceTest`/
  `RagAssistantConfigTest.naoAplicaFiltroNenhumQuandoRagCicloVidaFiltroServiceDevolveVazio`
  (recebendo `empty()`, nenhum dos dois pontos de leitura anexa filtro
  algum à busca — comportamento idêntico ao de antes da Etapa 16).
- Testes novos: 17 (`RagCicloVidaFiltroServiceTest`: 6, incluindo cache
  TTL respeitado/expirado; `PesquisaServiceTest`: +4 — OBSOLETO excluído,
  sem status continua aparecendo, filtro aplicado quando presente, nenhum
  filtro quando ausente; `RagAssistantConfigTest`: 4, novo arquivo, mesmos
  casos de `PesquisaServiceTest` mas pro bean de retrieval do chat;
  `IndexacaoServiceTest`: +1 (status real ≠ VIGENTE gravado como está, não
  sobrescrito); `ReindexacaoEmMassaControllerSecurityTest`: +2 (GET
  /status 403 pra não-ADMIN, 200 pra ADMIN); `DocumentoServiceComEstruturaTest`/
  outros: sem mudança — `isVigente()` é aditivo, não quebra nada existente).
- Suíte completa: 276/276 passando, 0 falhas, 0 erros (era 259).

### Etapa 17 — Reindexação automática em mudança de metadado (2026-10-10)

Gap único identificado (já estava documentado no código, Etapa 5):
`DocumentoService.atualizarStatusCicloVida` (endpoint ADMIN) troca
`status_ciclo_vida` SEM versionar o documento — de propósito, decisão
original — mas também nunca publicava `DocumentoAlteradoEvent`, então o
`IndexacaoListener` nunca reindexava; os chunks já indexados ficavam com
o `status_ciclo_vida` antigo no metadado (Etapa 14/pré-filtro da Etapa
16 dessincronizados do banco). Outros caminhos que mudam metadado
(categoria, tipo, confidencialidade — sempre via `atualizar()`/
`atualizarComEstrutura()`/criação) já versionam e já reindexam por
conta da Etapa 7+ — não precisaram de nenhuma mudança.

- **Correção**: `atualizarStatusCicloVida` agora publica
  `DocumentoAlteradoEvent(documento.getId(), documento.getVersaoAtual())`
  — mesma versão de sempre (não incrementa), só sinaliza pro
  `IndexacaoListener` reindexar. Sempre ligado (B4), sem flag de RAG.
- Teste novo: `atualizarStatusCicloVidaDisparaReindexacaoMesmoSemVersionar`
  (substituiu o antigo `verify(eventPublisher, never())...` que
  documentava o gap).
- Suíte completa: 277/277 passando, 0 falhas, 0 erros (era 276).

### Etapa 18 — Governança, backend (2026-10-10)

Decisão 9 ("sem e-mail; só cálculo de proxima_revisao, indicador visual
de 'revisão vencida' na UI e filtro de listagem; pendencias[] visíveis")
— esta etapa entrega a parte BACKEND: cálculo + filtro. O indicador
VISUAL é Etapa 19 (frontend, não começada). `pendencias[]` já é
persistido no `conteudo_estruturado` desde a Etapa 7/8 e já volta no
GET do documento — nada novo necessário aí.

- **Cálculo de `proximaRevisao`** (interpretação registrada aqui por
  não haver um gatilho/base de data explícita no plano original):
  recalculado em `aplicarMetadadosEstruturados` — toda vez que os
  metadados estruturados são aplicados (criação ou atualização via o
  caminho estruturado) — como `(dataVigencia ou hoje) +
  periodicidadeRevisaoMeses`. `dataVigencia` nunca é preenchida pelo
  DTO estruturado hoje (fica sempre `null`), então na prática a base é
  sempre "hoje" — se `dataVigencia` vier a ser preenchida numa etapa
  futura, passa a valer como base automaticamente. Sem
  `periodicidadeRevisaoMeses`, `proximaRevisao` fica `null` (nada a
  calcular).
- **Filtro de listagem**: `GET /documentos?revisaoVencida=true` (novo
  parâmetro, default `false` — comportamento de sempre quando ausente)
  — só documentos com `proximaRevisao` no passado. **A6**: documento
  `OBSOLETO` nunca conta como revisão vencida, mesmo com `proximaRevisao`
  vencida.
- Testes novos: 3, todos em `DocumentoServiceTest` (só vencidos aparecem
  com o filtro, OBSOLETO nunca aparece mesmo vencido, sem o filtro
  devolve todos independente da revisão). `DocumentoServiceComEstruturaTest`
  ganhou uma asserção nova (`proximaRevisao` calculada) no teste já
  existente, não um teste novo.
- Suíte completa: 280/280 passando, 0 falhas, 0 erros (era 277).

### Fix pós-Etapa-18 — `categoria_id` não é garantidamente não-nulo (commit `80fab78`, 2026-10-10)

Achado em teste manual live, logo após o deploy conjunto das Etapas
14-18: `IndexacaoService` assumia `documento.categoriaId` sempre
presente (não-nulo). A constraint `NOT NULL` usada como base pertencia
a uma tabela LEGADA homônima (`documentation`, `V1__baseline_schema.sql`),
não à tabela `documento` real (`V6__add_categoria_a_documento_e_usuario.sql`,
sem `NOT NULL`). Existe pelo menos 1 documento real sem categoria em
produção — toda vez que o job de segurança tentava reindexá-lo,
`NullPointerException`, log de erro recorrente, documento nunca
indexado/buscável.

- **Correção**: `categoria_id` tratado como os outros 3 metadados
  opcionais (omitido quando ausente, nunca usado no pré-filtro mesmo
  assim). `RagCicloVidaFiltroService` passa a checar SÓ
  `status_ciclo_vida IS NULL` pra detectar chunk órfão — `categoria_id`
  nulo é um estado legítimo (documento sem categoria), não indício de
  "nunca reindexado desde a Etapa 14".
- Teste novo: `indexaNormalmenteDocumentoSemCategoriaOmitindoOMetadado`.
- Confirmado ao vivo: documento foi de `ERRO` para `INDEXADO` após o
  redeploy, erro parou de recorrer.
- Suíte completa: 281/281 passando, 0 falhas, 0 erros.

### Verificação V1-V4 do RAG (pedida antes de fechar a Etapa 16, 2026-10-10)

**V1 — proteção × documento sem categoria**: confirmado ao vivo, `GET
/admin/reindexacao/status` → `{"chunksSemMetadado":0,...}` mesmo com o
documento sem categoria presente — a correção do fix acima (checar só
`status_ciclo_vida`) já resolve isso; `categoria_id` nulo nunca conta
como chunk órfão. Confirmado também via SQL direto nos chunks desse
documento: `categoria_id` vazio, `status_ciclo_vida='VIGENTE'`.

**V2 — visibilidade do documento sem categoria**: confirmado ao vivo
(listagem `/documentos` e busca `/documentos/busca`, com um usuário
USUARIO de teste restrito à categoria RH) que ADMIN vê o documento e
USUARIO/EDITOR não — `CategoriaAccessService.podeAcessarCategoria(null)`
é sempre `false` pra não-admin, sempre `true` pra admin, independente
do pré-filtro (que nunca toca `categoria_id`). Provado com teste nos
dois serviços de leitura (`PesquisaServiceTest`/`RagAssistantConfigTest`):
visibilidade idêntica com o pré-filtro presente ou ausente.
**Achado à parte** (fora do escopo do V2 original, documentado em
teste, não corrigido): em `RagAssistantConfig`, ADMIN pula
`acessivelPelaCategoria` por inteiro (branch `isAdmin()` separado) —
nunca chega a checar `isVigente()` pra admin; já `PesquisaService`
aplica `isVigente()` mesmo pra admin (só pula a checagem de categoria).
Resultado prático idêntico pro caso sem categoria (ambos mostram pro
admin), mas os dois serviços tratariam um documento OBSOLETO de forma
diferente pro ADMIN — decisão de produto a confirmar com o usuário, não
um bug óbvio.

**V3 — comparação com a flag ligada**: repetida com
`RAG_FILTROS_CICLO_VIDA_ENABLED=true`, 5 consultas variadas
("processo de vendas", "política de férias", "cadastro de produto",
"emissão de documentos fiscais", "processo de compras") × 2 perfis
(ADMIN; USUARIO de teste restrito à categoria Recursos Humanos) = 10
comparações. **Diff = 0 em todas as 10** — nenhum documento apareceu ou
sumiu em nenhuma combinação consulta/perfil com a flag ligada vs.
desligada. Flag revertida pra `false` ao final (usuário de teste
também removido).

**V4 — gatilhos da Etapa 17 (B4)**: dos 3 gatilhos combinados, 2 já
funcionavam (mudança de status via `atualizarStatusCicloVida`, Etapa
17; confirmação de rascunho estruturado via `criarComEstrutura`/
`atualizarComEstrutura`, cobertura de teste nova adicionada agora — o
código já publicava o evento desde a Etapa 13.6, só faltava a
verificação explícita). **1 gatilho tinha um gap real, corrigido**:
`atualizar()` (fluxo legado) setava `categoria_id` ANTES de checar o
hash do conteúdo — mudar só a categoria (conteúdo idêntico) atualizava
a coluna no banco mas nunca disparava reindexação (early return sem
publicar evento), deixando o metadado do chunk dessincronizado.
Corrigido: publica o evento (mesma versão, sem incrementar) quando a
categoria muda, mesmo sem nova versão de conteúdo; não publica quando
nada muda. Também corrigido: `proximaRevisao` só era recalculada em
`aplicarMetadadosEstruturados` (criação/atualização estruturada) — uma
atualização de CONTEÚDO pelo fluxo legado (sem bloco de metadados)
nunca recalculava, mesmo com `periodicidadeRevisaoMeses` já definido.
Movido pra dentro de `aplicarNovaVersao` (chamado por toda nova versão:
legado, estruturado, restauração), redundante-mas-inofensivo com a
chamada existente pro caminho estruturado.

- Testes novos: 6 (`atualizarSoACategoriaSemMudarOConteudoPreservaOEstruturadoNaoVersionaEDisparaReindexacao`,
  `atualizarSemMudarCategoriaNemConteudoNaoDisparaReindexacao`,
  `atualizarPeloFluxoLegadoComMudancaDeConteudoRecalculaProximaRevisao`
  em `DocumentoServiceTest`; 2 asserções de evento adicionadas aos
  testes existentes de `criarComEstrutura`/`atualizarComEstrutura` em
  `DocumentoServiceComEstruturaTest`; 2 em `PesquisaServiceTest`
  (visibilidade sem categoria, ADMIN/não-ADMIN); 2 em
  `RagAssistantConfigTest` (idem)).
- Suíte completa: 287/287 passando, 0 falhas, 0 erros (era 281).
