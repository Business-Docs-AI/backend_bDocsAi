# Configuração e custos

## Tabela de propriedades/variáveis de ambiente da IA

Todas vêm de `src/main/resources/application.yaml` (os perfis `application-dev.yaml`,
`application-prod.yaml` e `application-test.yaml` não sobrescrevem nada relacionado à IA).
Não há uma classe `@ConfigurationProperties` dedicada — toda configuração de IA é lida via
`@Value` espalhado pelas classes citadas.

| Propriedade | Variável de ambiente | Padrão | Para que serve |
|---|---|---|---|
| `app.ai.chat-provider` | `AI_CHAT_PROVIDER` | `gemini` | Provedor do modelo de chat: `gemini`, `anthropic` ou `openai` |
| `app.ai.embedding-provider` | `AI_EMBEDDING_PROVIDER` | `local` | Provedor de embedding: `local` (ONNX) ou `openai` |
| `app.ai.openai-api-key` | `OPENAI_API_KEY` | (vazio) | Chave da OpenAI |
| `app.ai.openai-chat-model` | `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | Nome do modelo de chat da OpenAI |
| `app.ai.embedding-model` | `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | Nome do modelo de embedding da OpenAI |
| `app.ai.embedding-dimension` | `OPENAI_EMBEDDING_DIMENSION` | `1536` | Dimensão do vetor para embeddings OpenAI |
| `app.ai.local-embedding-dimension` | — | `384` | Dimensão do vetor para o embedding local (ONNX) |
| `app.ai.anthropic-api-key` | `ANTHROPIC_API_KEY` | (vazio) | Chave da Anthropic |
| `app.ai.anthropic-chat-model` | `ANTHROPIC_CHAT_MODEL` | `claude-sonnet-5` | Nome do modelo de chat da Anthropic |
| `app.ai.gemini-api-key` | `GEMINI_API_KEY` | (vazio) | Chave do Gemini |
| `app.ai.gemini-chat-model` | `GEMINI_CHAT_MODEL` | `gemini-3.8-flash` | Nome do modelo de chat do Gemini |
| `app.reindexacao.intervalo-ms` | `REINDEXACAO_INTERVALO_MS` | `300000` | Intervalo do job de reindexação automática (pendentes/erro) |
| `app.chat.limite-caracteres-texto-grande` | `CHAT_LIMITE_CARACTERES_TEXTO_GRANDE` | `2000` | A partir de quantos caracteres um texto colado no chat é convertido para Markdown |
| `bdocs.documentacao-estruturada.enabled` | `DOCUMENTACAO_ESTRUTURADA_ENABLED` | `false` | **Feature flag 1** (ver abaixo) |
| `bdocs.documentacao-estruturada.provedores` | `DOCUMENTACAO_ESTRUTURADA_PROVEDORES` | `anthropic` | Lista (CSV) de provedores de chat em que o caminho estruturado fica disponível |
| `bdocs.documentacao-estruturada.geracao.max-tokens` | `..._GERACAO_MAX_TOKENS` | `16384` | `maxTokens` do `ChatModel` dedicado ao worker |
| `bdocs.documentacao-estruturada.geracao.timeout-segundos` | `..._TIMEOUT_SEGUNDOS` | `240` | Timeout do `ChatModel` dedicado ao worker |
| `bdocs.documentacao-estruturada.geracao.max-retries-http` | `..._MAX_RETRIES_HTTP` | `0` | Tentativas automáticas do cliente HTTP do LangChain4j por chamada (deliberadamente 0) |
| `bdocs.documentacao-estruturada.geracao.max-tentativas` | `..._MAX_TENTATIVAS` | `2` | Tentativas completas por rascunho antes de desistir (`ERRO_GERACAO`) |
| `bdocs.documentacao-estruturada.geracao.timeout-gerando-minutos` | `..._TIMEOUT_GERANDO_MINUTOS` | `5` | Tempo em `GERANDO` antes de o job de segurança considerar o rascunho "preso" |
| `bdocs.documentacao-estruturada.geracao.intervalo-job-ms` | `..._INTERVALO_JOB_MS` | `60000` | Intervalo do job de recuperação de gerações presas |
| `bdocs.documentacao-estruturada.geracao.material-max-caracteres` | `..._MATERIAL_MAX_CARACTERES` | `60000` | Tamanho máximo do material-fonte antes de rejeitar com erro |
| `bdocs.documentacao-estruturada.geracao.modelo-fake` | `..._MODELO_FAKE` | `false` | Modo de teste sem chamar a API real (nunca `true` em produção) |
| `bdocs.documentacao-estruturada.geracao.modelo-fake-atraso-ms` | `..._MODELO_FAKE_ATRASO_MS` | `5000` | Atraso artificial do modo modelo-fake |
| `bdocs.rag.filtros-ciclo-vida.enabled` | `RAG_FILTROS_CICLO_VIDA_ENABLED` | `false` | **Feature flag 2** (ver abaixo) |
| `bdocs.rag.filtros-ciclo-vida.cache-ttl-minutos` | `..._CACHE_TTL_MINUTOS` | `5` | TTL do cache da verificação de "chunks sem metadado" |

## As duas feature flags

### 1. `bdocs.documentacao-estruturada.enabled`

Lida por `config/FeatureFlags.java` **só no startup** — os beans de `AiServices` que
dependem dela são montados uma vez; mudar a flag em runtime não tem efeito até reiniciar a
aplicação. Com ela em `false` (padrão), nada do fluxo de documentação estruturada existe em
tempo de execução: o sistema se comporta exatamente como antes dessa funcionalidade.

Controla, via `@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name =
"enabled", havingValue = "true")`:
- os beans `MacroprocessoTools`, `DocumentoEstruturadoTools`, `chatModelGeracaoEstruturada`,
  `GeracaoEstruturadaService`, `GeracaoEstruturadaListener`, `GeracaoEstruturadaJob`;
- os controllers `DocumentoStatusCicloVidaController` e `ReindexacaoEmMassaController`.

O método `FeatureFlags.documentacaoEstruturadaHabilitadaPara(provedor)` soma essa flag **e**
a lista `bdocs.documentacao-estruturada.provedores` — usado em
`RagAssistantConfig.java:237` para decidir se `DocumentoEstruturadoTools` entra na lista de
ferramentas do assistente, de acordo com o provedor de chat ativo no momento (ver abaixo, o
problema do Gemini).

### 2. `bdocs.rag.filtros-ciclo-vida.enabled`

Lida diretamente via `@Value` em `ai/retrieval/RagCicloVidaFiltroService.java:49`. Controla
**só o lado de leitura/filtro** do ciclo de vida no RAG (pré-filtro na busca + checagem de
status no pós-filtro) — a gravação do metadado `status_ciclo_vida` nos chunks e os gatilhos
de reindexação continuam sempre ativos, independente desta flag. Com ela desligada (padrão),
`RagAssistantConfig` e `PesquisaService` se comportam exatamente como antes dessa
funcionalidade, para qualquer papel (inclusive ADMIN). Ver
[`02-rag-embeddings.md`](02-rag-embeddings.md) para o detalhe da proteção automática contra
chunks sem metadado.

## ChatModel do chat interativo × ChatModel do worker

`ai/generation/ChatModelConfig.java` define dois beans de `ChatModel`:

| | `chatModel` (`@Primary`) | `chatModelGeracaoEstruturada` |
|---|---|---|
| Usado por | chat interativo (todo o resto do sistema) | só o worker de geração estruturada |
| Provedor | o que estiver em `app.ai.chat-provider` (Gemini/Anthropic/OpenAI) | sempre Anthropic, fixo, ignora `app.ai.chat-provider` |
| `maxTokens` | `8192` (fixo, só no branch Anthropic) | `bdocs.documentacao-estruturada.geracao.max-tokens` (16384) |
| `timeout` | padrão do SDK | `bdocs.documentacao-estruturada.geracao.timeout-segundos` (240s) |
| `maxRetries` | padrão do SDK | `bdocs.documentacao-estruturada.geracao.max-retries-http` (0) |
| Cache de prompt | `cacheSystemMessages(true)`, `cacheTools(true)` (reduz custo/latência em conversas com várias idas e vindas de ferramentas) | não configurado |
| Logs | `logRequests(true)`, `logResponses(true)` | **sem** log de request/response — só um `ChatModelListener` que loga tokens/latência/`finishReason`, nunca o conteúdo |

O worker sempre usa Anthropic porque foi o único provedor validado para o tipo de
*tool-calling* que a geração estruturada exige — ver o problema do Gemini, abaixo.
`maxRetries=0` é deliberado: o próprio worker já tem seu mecanismo de tentativas
(`max-tentativas` + job de recuperação, ver
[`04-documentacao-estruturada.md`](04-documentacao-estruturada.md)); sem isso, o retry
padrão do LangChain4j multiplicaria silenciosamente o custo de cada tentativa (cada uma já
gera ~14 mil tokens de saída).

## Custos aproximados

Medido no projeto (`docs/plano-documentacao-estruturada.md`, linha 301): uma média de
**~13.897 tokens de saída** por geração de documento estruturado, com 5/5 sucesso de
validação em um teste de 5 gerações. Isso confirma a faixa de 10-15 mil tokens de saída por
documento estruturado mencionada nesta documentação.

Um trecho do mesmo histórico (linha 996-998) registra que o time rejeitou aumentar
`maxTokens`/timeout numa certa ocasião porque "~15k tokens de saída para um processo PEQUENO
deixa menos de 10% de margem" — ou seja, o custo por geração é tratado como uma restrição de
design deliberada, não um detalhe incidental.

## Boas práticas para não gastar créditos à toa em desenvolvimento

- **Use o modo modelo-fake** (`bdocs.documentacao-estruturada.geracao.modelo-fake=true`)
  para testar o fluxo de geração assíncrona (reserva, concorrência, polling, UI) sem chamar
  a Anthropic.
- **Nunca repita um teste manual já validado** contra a API real — o histórico do projeto
  (`docs/plano-documentacao-estruturada.md`, linha 20-32) registra um incidente real: "duas
  recargas de crédito da Anthropic em um único dia" causado por repetir investigações
  manuais já confirmadas.
- **Peça autorização explícita antes de qualquer chamada real à Anthropic** durante
  investigação/depuração, e estime o número de chamadas e tokens antes de pedir — essa é a
  regra de custo registrada no histórico do projeto (mesma referência acima).
- Os testes automatizados (JUnit) sempre usam um `ChatModel` fake/mockado — nunca uma chave
  real.

## Problema conhecido: Gemini e `tool-calling`

`gemini-3.8-flash` (um modelo "thinking"), através de `langchain4j-google-ai-gemini:1.18.0`,
falha em **qualquer** chamada de ferramenta feita através de `AiServices` — não é um
problema específico do DTO estruturado. Confirmado em
`docs/plano-documentacao-estruturada.md` (linhas 138-152 e 827-848) e documentado também em
comentário no próprio `application.yaml` (linhas 56-59). Mitigação aplicada:
`bdocs.documentacao-estruturada.provedores` tem como padrão só `anthropic`, excluindo o
Gemini desse caminho — mas o Gemini continua sendo o provedor **padrão do chat interativo**
(`app.ai.chat-provider=gemini`). *A confirmar*: por que o chat interativo tolera o Gemini
mesmo também usando `AiServices` com ferramentas (`ragAssistantComFerramentas`) — o histórico
do projeto não deixa isso totalmente explícito; pode valer a pena testar esse caminho com
Gemini antes de confiar cegamente nele em produção.

Ver mais em [`06-guia-pratico.md`](06-guia-pratico.md), seção de problemas conhecidos.
