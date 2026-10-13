# LangChain4j neste projeto

[LangChain4j](https://docs.langchain4j.dev/) é uma biblioteca Java que dá uma camada comum
para falar com modelos de linguagem (LLMs) de diferentes provedores (OpenAI, Anthropic,
Google, etc.) e para montar fluxos de RAG (explicado abaixo) sem reescrever a integração
com cada provedor na mão.

Versão usada neste projeto (`build.gradle`): `1.18.0` para os módulos principais
(`langchain4j`, `langchain4j-open-ai`, `langchain4j-anthropic`,
`langchain4j-google-ai-gemini`), `1.18.0-beta28` para `langchain4j-pgvector` e
`1.0.0-beta5` para `langchain4j-embeddings-all-minilm-l6-v2-q` (o modelo de embedding
local).

## RAG, em uma frase

RAG (*Retrieval-Augmented Generation*, "geração aumentada por recuperação") é a técnica de,
antes de perguntar algo a um LLM, buscar trechos relevantes de uma base de conhecimento e
colocá-los dentro da pergunta como contexto — assim o modelo responde com base em dados
reais da empresa, em vez de "inventar" (alucinar) uma resposta. Neste projeto, a base de
conhecimento são os próprios documentos cadastrados no sistema.

## Conceitos usados, com onde encontrá-los no código

### `ChatModel`
Representa a conexão com um modelo de chat (LLM) de um provedor específico. Neste projeto
há dois beans de `ChatModel`:
- `chatModel` (`@Primary`), usado pelo chat interativo — `ai/generation/ChatModelConfig.java:32`.
- `chatModelGeracaoEstruturada`, dedicado ao worker de geração de documento estruturado,
  sempre via Anthropic — `ai/generation/ChatModelConfig.java:105`.

### `AiServices`
É a "fábrica" do LangChain4j que, a partir de uma interface Java simples, gera em runtime
uma implementação que conversa com o `ChatModel`, injeta contexto de RAG, gerencia memória
de conversa e expõe ferramentas (`@Tool`) ao modelo. Usado em dois lugares:
- `ai/generation/RagAssistantConfig.java` — monta os beans `ragAssistantSomenteLeitura`
  (linha 195) e `ragAssistantComFerramentas` (linha 224), ambos a partir da interface
  `RagAssistant` (`ai/generation/RagAssistant.java`).
- `document/service/GeracaoEstruturadaService.java:221` — monta, dentro do método
  `gerarDocumentoValidado`, um `AiServices` local (interface `Assistente`, linha 114) usado
  só pelo worker de geração estruturada.

### `@Tool`
Anotação colocada em métodos Java comuns para expor essa função ao modelo como uma
"ferramenta" que ele pode decidir chamar (*tool calling*/*function calling*). O texto
passado para `@Tool("...")` é a descrição que o modelo lê para decidir quando usar aquela
ferramenta — por isso os textos nas tools deste projeto são longos e dão instruções
explícitas de uso. Veja a tabela completa de tools em
[`03-chat-e-tools.md`](03-chat-e-tools.md).

### `@P`
Anotação (`dev.langchain4j.agent.tool.P`) usada nos parâmetros de um método `@Tool` para
descrever, em linguagem natural, o que aquele parâmetro espera — ex.:
`document/tool/DocumentoTools.java:86`, no parâmetro `consulta` de `buscarDocumentos`.

### `ChatMemory` / `ChatMemoryProvider`
`ChatMemory` guarda o histórico de mensagens de uma conversa que é enviado ao modelo a cada
chamada; `ChatMemoryProvider` é a fábrica que cria (ou recupera) a `ChatMemory` certa para
cada `memoryId`. Neste projeto:
- O chat interativo usa `MessageWindowChatMemory` com janela de 20 mensagens, configurado
  em `ai/generation/RagAssistantConfig.java:184` (bean `chatMemoryProvider`).
- O worker de geração estruturada usa sua própria `MessageWindowChatMemory` (mesma janela
  de 20 mensagens), mas **sem** persistência — é só a "conversa" interna entre o worker e o
  modelo dentro de uma única geração, criada em
  `document/service/GeracaoEstruturadaService.java:219`.

### `ChatMemoryStore`
A interface que o LangChain4j usa para persistir/ler as mensagens de uma `ChatMemory`.
Implementada por `chat/service/PersistentChatMemoryStore.java`, que lê e escreve na tabela
`chat_mensagem` (ver [`03-chat-e-tools.md`](03-chat-e-tools.md) para o porquê de também
existir um cache em memória por turno nessa classe).

### `EmbeddingModel`
Transforma um texto em um vetor numérico (*embedding*) que captura seu significado — textos
com significado parecido geram vetores próximos entre si. Configurado em
`ai/embeddings/EmbeddingConfig.java:27` (bean `embeddingModel`), podendo ser local (ONNX,
sem chamada de rede) ou via API da OpenAI — ver [`02-rag-embeddings.md`](02-rag-embeddings.md).

### `EmbeddingStore` (pgvector)
Onde os embeddings são guardados e buscados por similaridade. Este projeto usa o
[pgvector](https://github.com/pgvector/pgvector) (extensão do PostgreSQL para vetores)
através do `PgVectorEmbeddingStore` do LangChain4j, configurado em
`ai/embeddings/EmbeddingConfig.java:50` (bean `embeddingStore`), apontando para a tabela
`documento_embedding`.

### `ContentRetriever`
Abstração do LangChain4j para "dado uma pergunta (`Query`), devolva os trechos de conteúdo
relevantes". Este projeto implementa isso em cima do `EmbeddingStoreContentRetriever`
(busca por similaridade no pgvector), com uma camada própria de filtro por cima — ver os
beans `documentoContentRetrieverBruto` e `documentoContentRetriever` em
`ai/generation/RagAssistantConfig.java:53` e `:83`.

### `Filter`
Interface do LangChain4j (`dev.langchain4j.store.embedding.filter.Filter`) para filtrar
resultados de busca vetorial por metadado (ex.: "só documentos com `status_ciclo_vida` =
`VIGENTE`"). Construído com `MetadataFilterBuilder` — usado em
`ai/retrieval/RagCicloVidaFiltroService.java:68` e em `ai/ingestion/IndexacaoService.java:95`.
Importante: a versão 1.18.0 usada aqui **não tem um filtro `IS NULL`** (só
`IsEqualTo`/`IsNotEqualTo`/`IsGreaterThan...`/`IsIn`/`IsNotIn`/`ContainsString` +
`And`/`Or`/`Not`, confirmado no comentário de `RagCicloVidaFiltroService.java:17-18`) — isso
molda boa parte do desenho do pré-filtro, detalhado em
[`02-rag-embeddings.md`](02-rag-embeddings.md).

### `RetrievalAugmentor`
Orquestra o fluxo de RAG completo: chama o `ContentRetriever`, depois o `ContentInjector`,
e devolve a mensagem final já com o contexto embutido. Bean `retrievalAugmentor` em
`ai/generation/RagAssistantConfig.java:174`, usando o `DefaultRetrievalAugmentor` do
LangChain4j.

### `ContentInjector`
Decide *como* o conteúdo recuperado é inserido na mensagem enviada ao modelo. Este projeto
usa um `DefaultContentInjector` com um `PromptTemplate` customizado — bean `contentInjector`
em `ai/generation/RagAssistantConfig.java:156` — porque o template padrão do LangChain4j
trata os trechos recuperados como instrução obrigatória, o que travava o assistente quando
os trechos não tinham relação com o pedido (ver o comentário nas linhas 148–154 do mesmo
arquivo, e mais detalhes em [`03-chat-e-tools.md`](03-chat-e-tools.md)).

### `@Description`
Anotação (`dev.langchain4j.model.output.structured.Description`) usada em cada campo de um
`record` para descrever, em linguagem natural, o que aquele campo representa — é a partir
dela que o LangChain4j monta o schema de *structured output* que o modelo precisa preencher.
Usada em todos os campos de `document/dto/estruturado/DocumentoEstruturadoDTO.java`, o DTO
central da geração de documento estruturado (ver
[`04-documentacao-estruturada.md`](04-documentacao-estruturada.md)).

### `DocumentSplitter` / `DocumentSplitters`
Interface e fábrica de utilitários do LangChain4j para dividir um texto longo em pedaços
menores (*chunks*). Este projeto usa `DocumentSplitters.recursive(maxSize, maxOverlap)` em
`ai/ingestion/IndexacaoService.java:103` — ver detalhes de tamanho/overlap em
[`02-rag-embeddings.md`](02-rag-embeddings.md).

### `ChatModelListener`
Interface para observar chamadas feitas a um `ChatModel` (request, response, erro) sem
alterar o fluxo — usada só no `ChatModel` do worker de geração estruturada, para registrar
em log quantos tokens de entrada/saída cada geração consumiu, sem nunca logar o conteúdo
(classe `GeracaoEstruturadaCustoListener`, interna a
`ai/generation/ChatModelConfig.java:136`).

## O que **não** é usado (a confirmar se muda no futuro)

- Não há `QueryTransformer` nem reescrita de pergunta com base no histórico da conversa —
  "buscar com histórico" neste projeto significa algo diferente: incluir documentos
  não-vigentes (obsoletos/em revisão) na busca, não reescrever a pergunta com o histórico da
  conversa. Ver a tool `buscarDocumentosIncluindoHistorico` em
  [`03-chat-e-tools.md`](03-chat-e-tools.md).
- Não há *reranking* (um segundo modelo reordenando os resultados da busca) — a ordenação é
  só pelo score de similaridade do pgvector.
