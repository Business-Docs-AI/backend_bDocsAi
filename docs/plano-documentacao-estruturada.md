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
