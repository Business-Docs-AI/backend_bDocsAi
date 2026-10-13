# Guia prático

Passo a passo para tarefas comuns ao mexer na camada de IA, e um glossário dos termos
usados nesta documentação.

## Adicionar uma tool nova

1. Decida se a tool é de uso geral (chat normal) ou específica da documentação estruturada.
   - Chat normal: adicione um método `@Tool` numa classe existente (`DocumentoTools`,
     `CategoriaTools`) ou crie uma classe nova anotada com `@Component`.
   - Documentação estruturada: siga o padrão de `MacroprocessoTools`/
     `DocumentoEstruturadoTools` — anote a classe com
     `@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled",
     havingValue = "true")`.
2. Escreva uma descrição clara e longa em `@Tool("...")` — é o único texto que o modelo lê
   para decidir quando chamar aquela ferramenta. Descreva também **quando não usar** (ex.:
   "use X em vez disso quando...").
3. Anote cada parâmetro com `@P("...")` explicando o que se espera.
4. Se a tool grava algo (criação/atualização), siga o padrão "preparar → confirmar em turno
   separado": nunca grave o resultado final diretamente — grave um rascunho e exija
   confirmação num turno posterior, usando `ConversaContextHolder.atual()` (ver
   [`03-chat-e-tools.md`](03-chat-e-tools.md)).
5. Adicione a nova classe de tools à lista `.tools(...)` do bean `ragAssistantComFerramentas`
   em `ai/generation/RagAssistantConfig.java` (como parâmetro do método, injetado pelo
   Spring).
6. Atualize a tabela de tools em [`03-chat-e-tools.md`](03-chat-e-tools.md).

## Alterar um prompt

- Chat interativo: edite as constantes `TEXTO`/`TEXTO_COM_FERRAMENTAS` em
  `ai/prompt/RagSystemPrompt.java`.
- Worker de geração estruturada: edite `document/service/GeracaoEstruturadaSystemPrompt.java`.

Depois de mudar um prompt que envolve ferramentas, teste manualmente o fluxo completo de
propor → confirmar (ou, para o worker, rode em modo `modelo-fake` primeiro, depois valide
uma geração real) — mudanças de texto podem alterar o comportamento de chamada de
ferramentas de forma sutil.

## Trocar o modelo/provedor de chat

Altere `app.ai.chat-provider` (env `AI_CHAT_PROVIDER`) para `gemini`, `anthropic` ou
`openai` — não requer mudança de código (`ai/generation/ChatModelConfig.java`). Lembre-se:
- O worker de geração estruturada **ignora** essa propriedade — sempre usa Anthropic (ver
  [`05-configuracao-e-custos.md`](05-configuracao-e-custos.md)).
- Se o provedor escolhido não estiver em `bdocs.documentacao-estruturada.provedores`, as
  ferramentas de documentação estruturada somem do chat para esse provedor (ver o problema
  do Gemini no mesmo documento).

## Trocar o provedor de embedding

Altere `app.ai.embedding-provider` para `local` ou `openai`
(`ai/embeddings/EmbeddingConfig.java`). **Depois de trocar, rode uma reindexação em massa**
(`POST /admin/reindexacao`, ADMIN, exige a flag `bdocs.documentacao-estruturada.enabled`
ligada) — os vetores já gravados ficam com a dimensão/geometria do provedor anterior e não
são comparáveis aos do novo (ver [`02-rag-embeddings.md`](02-rag-embeddings.md)).

## Reindexar documentos

- **Um documento específico**: `POST /documentos/{id}/reindexar` (ADMIN) — marca
  `status_indexacao = PENDENTE` e deixa o job automático
  (`document/job/ReindexacaoJob.java`, a cada `app.reindexacao.intervalo-ms`) pegar.
- **Todos os documentos**: `POST /admin/reindexacao` (ADMIN, exige a flag de documentação
  estruturada ligada) — dispara `ReindexacaoEmMassaService.reindexarTodos()` de forma
  assíncrona.
- **Verificar se ainda há chunks sem metadado novo** (bloqueando o pré-filtro de ciclo de
  vida): `GET /admin/reindexacao/status`.

## Investigar por que um documento não aparece no RAG

Siga esta ordem:

1. **Indexação terminou?** Confira `status_indexacao` do documento
   (`DocumentoEntity.statusIndexacao`) — se `PENDENTE` ou `ERRO`, olhe os logs de
   `ai/ingestion/IndexacaoService.java` (`log.error` na linha 82) para a causa.
2. **Score baixo demais?** O retriever automático do chat exige score ≥ `0.75`
   (`RagAssistantConfig.SCORE_MINIMO_RECUPERACAO`) — mais alto que a busca explícita (`0.5`,
   `PesquisaService.SCORE_MINIMO`). Teste primeiro via `GET /documentos/busca?q=...` ou a
   tool `buscarDocumentos`, que usam o threshold mais baixo.
3. **Documento não está vigente?** Se `bdocs.rag.filtros-ciclo-vida.enabled` está ligada, só
   documentos `VIGENTE` (ou sem status, tratado como vigente) aparecem por padrão. Use
   `buscarDocumentosIncluindoHistorico` para confirmar, ou confira
   `DocumentoEntity.statusCicloVida`.
4. **Categoria?** O usuário (ou a sessão de teste) precisa estar vinculado à categoria do
   documento (`usuario_categoria`) — ADMIN não tem essa restrição.
5. **Chunk sem metadado?** `GET /admin/reindexacao/status` mostra quantos chunks ainda não
   têm `status_ciclo_vida` — enquanto houver algum, o pré-filtro de vigência fica
   automaticamente desligado (mas o pós-filtro continua valendo).

## Testar sem gastar créditos de API

- Geração de documento estruturado: ligue
  `bdocs.documentacao-estruturada.geracao.modelo-fake=true` — nenhuma chamada real é feita.
- Testes automatizados (JUnit): sempre usam um `ChatModel`/`EmbeddingModel` fake — nunca
  configure uma chave real em `application-test.yaml`.
- Para testar o chat interativo sem custo de um provedor pago, use o Gemini (gratuito) como
  `app.ai.chat-provider` — mas veja a ressalva sobre tool-calling abaixo.

## Problemas conhecidos

- **Gemini + tool-calling via `AiServices` não funciona** (`gemini-3.8-flash` +
  `langchain4j-google-ai-gemini:1.18.0`) — não é específico do DTO estruturado, é qualquer
  ferramenta. Por isso a documentação estruturada tem `anthropic` como único provedor padrão
  em `bdocs.documentacao-estruturada.provedores`. Se for investigar esse problema, comece
  por `docs/plano-documentacao-estruturada.md` (seção "Problema separado: Gemini").
- **LangChain4j 1.18.0 não tem filtro `IS NULL`** — por isso o pré-filtro de ciclo de vida do
  RAG (`ai/retrieval/RagCicloVidaFiltroService.java`) precisa de uma consulta SQL direta para
  decidir se é seguro aplicar o filtro, em vez de simplesmente filtrar por
  "`status_ciclo_vida` não é nulo".
- **Comentários desatualizados em `DocumentoEntity.java`/`DocumentoService.java`** sobre
  `macroprocessoId`/`processoPaiId` "nunca serem setados" — isso já mudou com a geração
  estruturada (ver [`04-documentacao-estruturada.md`](04-documentacao-estruturada.md)); os
  comentários no código não foram atualizados.
- **Chunk-size "em tokens" na verdade é em caracteres** — ver nota em
  [`02-rag-embeddings.md`](02-rag-embeddings.md).

## Glossário

| Termo | Significado |
|---|---|
| **RAG** (*Retrieval-Augmented Generation*) | Buscar trechos relevantes de uma base de conhecimento e incluí-los no prompt antes de perguntar ao LLM, para respostas baseadas em dados reais em vez de "inventadas". |
| **Embedding** | Vetor numérico que representa o significado de um texto — textos parecidos geram vetores próximos. |
| **Chunk** | Pedaço de texto (um trecho de um documento) pequeno o bastante para virar um embedding útil para busca. |
| **Vetor** | O embedding em si — uma lista de números (384 ou 1536 posições, neste projeto, dependendo do provedor). |
| **pgvector** | Extensão do PostgreSQL que permite guardar vetores numa coluna e buscar por similaridade (vizinho mais próximo) com SQL. |
| **Tool** (ferramenta) | Uma função Java anotada com `@Tool` que o modelo de IA pode decidir chamar durante uma conversa. |
| **System prompt** | A instrução inicial, fixa, que define o comportamento/papel do assistente de IA antes de qualquer mensagem do usuário. |
| **Token** | Unidade de texto que um LLM processa (aproximadamente um pedaço de palavra) — custo e limites de um LLM geralmente são medidos em tokens. |
| **AiServices** | Mecanismo do LangChain4j que gera, a partir de uma interface Java, uma implementação que conversa com um LLM, usando RAG/memória/ferramentas. |
| **ChatMemory** | O histórico de mensagens de uma conversa, mantido para ser reenviado ao modelo a cada chamada. |
| **Feature flag** | Uma propriedade de configuração booleana que liga/desliga uma funcionalidade inteira sem precisar remover código. |
| **Rascunho** | Uma proposta de criação/atualização (de documento ou categoria) feita pela IA, ainda não confirmada pelo usuário. |
| **Documento vigente** | Um documento cujo `status_ciclo_vida` é `VIGENTE` (ou nulo, tratado como vigente por padrão) — o que normalmente aparece nas buscas. |
