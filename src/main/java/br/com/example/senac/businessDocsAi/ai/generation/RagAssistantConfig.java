package br.com.example.senac.businessDocsAi.ai.generation;

import br.com.example.senac.businessDocsAi.ai.prompt.RagSystemPrompt;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.categories.tool.CategoriaTools;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.tool.DocumentoTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

@Configuration
public class RagAssistantConfig {

    // Busca mais bruto do que o necessário (MAX_TRECHOS_RECUPERADOS) porque parte dos
    // resultados pode ser descartada pelo filtro de categoria logo abaixo.
    private static final int MAX_TRECHOS_BRUTOS = 15;
    private static final int MAX_TRECHOS_RECUPERADOS = 5;
    private static final double SCORE_MINIMO_RECUPERACAO = 0.6;
    private static final int JANELA_MEMORIA_MENSAGENS = 20;

    @Bean
    public ContentRetriever documentoContentRetrieverBruto(
            PgVectorEmbeddingStore embeddingStore,
            EmbeddingModel embeddingModel
    ) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(MAX_TRECHOS_BRUTOS)
                .minScore(SCORE_MINIMO_RECUPERACAO)
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
                .map(documento -> categoriaAccessService.podeAcessarCategoria(documento.getCategoriaId()))
                .orElse(false);
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
            ContentRetriever documentoContentRetriever,
            ChatMemoryProvider chatMemoryProvider
    ) {
        return AiServices.builder(RagAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(documentoContentRetriever)
                .chatMemoryProvider(chatMemoryProvider)
                .systemMessageProvider(memoryId -> RagSystemPrompt.TEXTO)
                .build();
    }

    // Usado para EDITOR/ADMIN: ganha as ferramentas de buscar/propor criação-atualização de
    // documento. Nenhuma delas grava direto — DocumentoTools só efetiva a escrita depois de
    // confirmação num turno posterior (ver DocumentoTools/ConversaContextHolder). Sem
    // ferramenta nenhuma de excluir/restaurar/reindexar.
    @Bean
    public RagAssistant ragAssistantComFerramentas(
            ChatModel chatModel,
            ContentRetriever documentoContentRetriever,
            ChatMemoryProvider chatMemoryProvider,
            DocumentoTools documentoTools,
            CategoriaTools categoriaTools
    ) {
        return AiServices.builder(RagAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(documentoContentRetriever)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(documentoTools, categoriaTools)
                .systemMessageProvider(memoryId -> RagSystemPrompt.TEXTO_COM_FERRAMENTAS)
                .build();
    }
}
