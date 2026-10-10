package br.com.example.senac.businessDocsAi.ai.generation;

import br.com.example.senac.businessDocsAi.ai.prompt.RagSystemPrompt;
import br.com.example.senac.businessDocsAi.ai.retrieval.RagCicloVidaFiltroService;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.categories.tool.CategoriaTools;
import br.com.example.senac.businessDocsAi.config.FeatureFlags;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.tool.DocumentoEstruturadoTools;
import br.com.example.senac.businessDocsAi.document.tool.DocumentoTools;
import br.com.example.senac.businessDocsAi.document.tool.MacroprocessoTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Configuration
public class RagAssistantConfig {

    // Busca mais bruto do que o necessário (MAX_TRECHOS_RECUPERADOS) porque parte dos
    // resultados pode ser descartada pelo filtro de categoria logo abaixo.
    private static final int MAX_TRECHOS_BRUTOS = 15;
    private static final int MAX_TRECHOS_RECUPERADOS = 5;
    // 0.6 deixava passar trechos só vagamente parecidos (vocabulário/estrutura de texto
    // empresarial em comum, sem relação de assunto de verdade) — confirmado na prática: um
    // texto sobre emissão de nota fiscal no ERP recuperou trechos de política de férias e de
    // senhas. Resultado: o modelo, instruído a "responder usando" esses trechos, travava
    // pedindo pro usuário esclarecer em vez de seguir o fluxo de criação de documento.
    private static final double SCORE_MINIMO_RECUPERACAO = 0.75;
    private static final int JANELA_MEMORIA_MENSAGENS = 20;

    @Bean
    public ContentRetriever documentoContentRetrieverBruto(
            PgVectorEmbeddingStore embeddingStore,
            EmbeddingModel embeddingModel,
            RagCicloVidaFiltroService ragCicloVidaFiltroService
    ) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(MAX_TRECHOS_BRUTOS)
                .minScore(SCORE_MINIMO_RECUPERACAO)
                // Etapa 16: dynamicFilter (não filter() estático) porque a decisão de
                // aplicar o pré-filtro depende do estado ATUAL do banco (flag + metadados
                // completos, com cache de poucos minutos) — reavaliado a cada busca, nunca
                // fixado na criação do bean.
                .dynamicFilter(query -> ragCicloVidaFiltroService.filtroStatusVigente().orElse(null))
                .build();
    }

    // Filtra o que o retriever bruto devolve pelas categorias que o usuário autenticado NO
    // MOMENTO DA PERGUNTA pode acessar (ADMIN não tem restrição). Sem isso, o RAG vazaria
    // trechos de documentos de categorias que o usuário não deveria ver.
    @Bean
    public ContentRetriever documentoContentRetriever(
            ContentRetriever documentoContentRetrieverBruto,
            IDocumentoRepository documentoRepository,
            CategoriaAccessService categoriaAccessService
    ) {
        return query -> {
            List<Content> conteudos = documentoContentRetrieverBruto.retrieve(query);

            if (categoriaAccessService.isAdmin()) {
                return conteudos.stream().limit(MAX_TRECHOS_RECUPERADOS).toList();
            }

            return conteudos.stream()
                    .filter(content -> acessivelPelaCategoria(content, documentoRepository, categoriaAccessService))
                    .limit(MAX_TRECHOS_RECUPERADOS)
                    .toList();
        };
    }

    private static boolean acessivelPelaCategoria(
            Content content, IDocumentoRepository documentoRepository, CategoriaAccessService categoriaAccessService
    ) {
        UUID documentoId = content.textSegment().metadata().getUUID("documento_id");

        if (documentoId == null) {
            return false;
        }

        return documentoRepository.findById(documentoId)
                .filter(documento -> !documento.isDeletado())
                // Etapa 16 (C4): confere o status NO BANCO (fonte da verdade), nunca no
                // metadado do chunk — mesma regra de PesquisaService.documentoAcessivel.
                .filter(DocumentoEntity::isVigente)
                .map(documento -> categoriaAccessService.podeAcessarCategoria(documento.getCategoriaId()))
                .orElse(false);
    }

    // O template padrão do langchain4j injeta os trechos recuperados como uma instrução
    // direta ("Answer using the following information: ..."), como se o modelo fosse
    // OBRIGADO a usá-los — isso confundia o assistente mesmo quando os trechos recuperados
    // não tinham nada a ver com o pedido (ex.: pedir pra CRIAR um documento sobre um assunto
    // novo), fazendo-o travar perguntando o que fazer com "documentos sem relação" em vez de
    // simplesmente seguir o fluxo normal de criação. Este template deixa claro que os
    // trechos são só uma referência opcional, a ignorar quando não forem relevantes.
    @Bean
    public ContentInjector contentInjector() {
        return DefaultContentInjector.builder()
                .promptTemplate(PromptTemplate.from("""
                        {{userMessage}}

                        Trechos de documentação que PODEM (ou não) ser relevantes para esta \
                        mensagem, encontrados por busca semântica na base de documentos:
                        {{contents}}

                        Use esses trechos SOMENTE se forem realmente relevantes para responder \
                        à pergunta ou para a ação pedida. Se não tiverem relação com o que foi \
                        pedido (por exemplo, se o usuário está pedindo para criar ou atualizar \
                        um documento sobre um assunto diferente), ignore-os completamente — não \
                        pergunte ao usuário o que fazer com eles, nem mencione que eles existem."""))
                .build();
    }

    @Bean
    public RetrievalAugmentor retrievalAugmentor(
            ContentRetriever documentoContentRetriever, ContentInjector contentInjector
    ) {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(documentoContentRetriever)
                .contentInjector(contentInjector)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(JANELA_MEMORIA_MENSAGENS)
                .chatMemoryStore(chatMemoryStore)
                .build();
    }

    // Usado para USUARIO: sem .tools(...), então não tem como alterar documentos — só lê o
    // contexto recuperado e responde.
    @Bean
    public RagAssistant ragAssistantSomenteLeitura(
            ChatModel chatModel,
            RetrievalAugmentor retrievalAugmentor,
            ChatMemoryProvider chatMemoryProvider
    ) {
        return AiServices.builder(RagAssistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemoryProvider(chatMemoryProvider)
                .systemMessageProvider(memoryId -> RagSystemPrompt.TEXTO)
                .build();
    }

    // Usado para EDITOR/ADMIN: ganha as ferramentas de buscar/propor criação-atualização de
    // documento. Nenhuma delas grava direto — DocumentoTools só efetiva a escrita depois de
    // confirmação num turno posterior (ver DocumentoTools/ConversaContextHolder). Sem
    // ferramenta nenhuma de excluir/restaurar/reindexar.
    //
    // MacroprocessoTools só existe como bean com a feature flag ligada — por isso a injeção
    // é Optional aqui: sem isso, o Spring falharia ao montar este bean com a flag desligada
    // (NoSuchBeanDefinition). Flag é lida só no startup — mudar em runtime não tem efeito
    // até reiniciar (os beans de AiServices são montados uma vez).
    //
    // DocumentoEstruturadoTools (Etapa 13.3) tem um gate ADICIONAL: além do bean exigir a
    // flag ligada, só é ADICIONADA à lista de tools quando o provedor de chat ATIVO também
    // está habilitado pro caminho estruturado (FeatureFlags.documentacaoEstruturadaHabilitadaPara
    // — decisão A3). Isso evita oferecer até a tool LEVE a um provedor onde o próprio
    // tool-calling já quebra (Gemini — ver plano, "Problema separado: Gemini", Etapa 10).
    @Bean
    public RagAssistant ragAssistantComFerramentas(
            ChatModel chatModel,
            RetrievalAugmentor retrievalAugmentor,
            ChatMemoryProvider chatMemoryProvider,
            DocumentoTools documentoTools,
            CategoriaTools categoriaTools,
            Optional<MacroprocessoTools> macroprocessoToolsOpt,
            Optional<DocumentoEstruturadoTools> documentoEstruturadoToolsOpt,
            FeatureFlags featureFlags,
            @Value("${app.ai.chat-provider}") String provedorAtual
    ) {
        List<Object> tools = new ArrayList<>(List.of(documentoTools, categoriaTools));
        macroprocessoToolsOpt.ifPresent(tools::add);
        if (featureFlags.documentacaoEstruturadaHabilitadaPara(provedorAtual)) {
            documentoEstruturadoToolsOpt.ifPresent(tools::add);
        }

        return AiServices.builder(RagAssistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(tools.toArray())
                .systemMessageProvider(memoryId -> RagSystemPrompt.TEXTO_COM_FERRAMENTAS)
                .build();
    }
}
