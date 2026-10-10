package br.com.example.senac.businessDocsAi.ai.generation;

import br.com.example.senac.businessDocsAi.ai.retrieval.RagCicloVidaFiltroService;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Etapa 16 — {@code documentoContentRetrieverBruto} (pré-filtro) e
 * {@code documentoContentRetriever} (pós-filtro, categoria + status). Os métodos @Bean são
 * chamados direto (sem contexto Spring) — são só fábricas de objeto, testáveis como métodos
 * Java comuns.
 */
@ExtendWith(MockitoExtension.class)
class RagAssistantConfigTest {

    private final RagAssistantConfig config = new RagAssistantConfig();

    @Mock
    private PgVectorEmbeddingStore embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private RagCicloVidaFiltroService ragCicloVidaFiltroService;

    @Mock
    private IDocumentoRepository documentoRepository;

    @Mock
    private CategoriaAccessService categoriaAccessService;

    @BeforeEach
    void setUp() {
        lenient().when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(Embedding.from(new float[]{0.1f, 0.2f})));
        lenient().when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of()));
    }

    @Test
    void aplicaOPreFiltroQuandoRagCicloVidaFiltroServiceDevolveUm() {
        Filter filtro = MetadataFilterBuilder.metadataKey("status_ciclo_vida").isEqualTo("VIGENTE");
        when(ragCicloVidaFiltroService.filtroStatusVigente()).thenReturn(Optional.of(filtro));

        ContentRetriever retriever = config.documentoContentRetrieverBruto(embeddingStore, embeddingModel, ragCicloVidaFiltroService);
        retriever.retrieve(Query.from("consulta"));

        ArgumentCaptor<EmbeddingSearchRequest> captor = ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore).search(captor.capture());
        assertThat(captor.getValue().filter()).isEqualTo(filtro);
    }

    // Mandatório (Etapa 16): com metadados incompletos (chunks legados), o pré-filtro fica
    // desligado — RagCicloVidaFiltroServiceTest já prova que isso vale com a flag ligada OU
    // desligada; aqui confirma que documentoContentRetrieverBruto, ao receber empty, nunca
    // anexa filtro nenhum à busca — mesmo comportamento de antes da Etapa 16.
    @Test
    void naoAplicaFiltroNenhumQuandoRagCicloVidaFiltroServiceDevolveVazio() {
        when(ragCicloVidaFiltroService.filtroStatusVigente()).thenReturn(Optional.empty());

        ContentRetriever retriever = config.documentoContentRetrieverBruto(embeddingStore, embeddingModel, ragCicloVidaFiltroService);
        retriever.retrieve(Query.from("consulta"));

        ArgumentCaptor<EmbeddingSearchRequest> captor = ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore).search(captor.capture());
        assertThat(captor.getValue().filter()).isNull();
    }

    @Test
    void posFiltroDescartaDocumentoObsoletoMesmoComAcessoDeCategoria() {
        UUID documentoObsoleto = UUID.randomUUID();
        DocumentoEntity obsoleto = documento(documentoObsoleto, StatusCicloVida.OBSOLETO);
        when(documentoRepository.findById(documentoObsoleto)).thenReturn(Optional.of(obsoleto));
        lenient().when(categoriaAccessService.isAdmin()).thenReturn(false);
        lenient().when(categoriaAccessService.podeAcessarCategoria(any())).thenReturn(true);

        ContentRetriever bruto = contentRetrieverBrutoComUmResultado(documentoObsoleto);
        ContentRetriever retriever = config.documentoContentRetriever(bruto, documentoRepository, categoriaAccessService);

        List<Content> conteudos = retriever.retrieve(Query.from("consulta"));

        assertThat(conteudos).isEmpty();
    }

    // "Vigente por padrão" (decisão 7/C4): sem status definido, o documento continua
    // aparecendo — nunca some do RAG por falta de metadado.
    @Test
    void posFiltroMantemDocumentoSemStatusCicloVidaDefinido() {
        UUID documentoLegado = UUID.randomUUID();
        DocumentoEntity legado = documento(documentoLegado, null);
        when(documentoRepository.findById(documentoLegado)).thenReturn(Optional.of(legado));
        lenient().when(categoriaAccessService.isAdmin()).thenReturn(false);
        when(categoriaAccessService.podeAcessarCategoria(any())).thenReturn(true);

        ContentRetriever bruto = contentRetrieverBrutoComUmResultado(documentoLegado);
        ContentRetriever retriever = config.documentoContentRetriever(bruto, documentoRepository, categoriaAccessService);

        List<Content> conteudos = retriever.retrieve(Query.from("consulta"));

        assertThat(conteudos).hasSize(1);
    }

    private ContentRetriever contentRetrieverBrutoComUmResultado(UUID documentoId) {
        Metadata metadata = new Metadata().put("documento_id", documentoId).put("titulo", "Doc").put("secao", "intro");
        TextSegment segmento = TextSegment.from("texto", metadata);
        return query -> List.of(Content.from(segmento));
    }

    private DocumentoEntity documento(UUID id, StatusCicloVida status) {
        DocumentoEntity documento = new DocumentoEntity();
        documento.setId(id);
        documento.setDeletado(false);
        documento.setCategoriaId(1L);
        documento.setStatusCicloVida(status);
        return documento;
    }
}
