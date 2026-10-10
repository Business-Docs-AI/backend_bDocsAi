package br.com.example.senac.businessDocsAi.ai.retrieval;

import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.document.dto.ResultadoBuscaDTO;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusCicloVida;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
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

@ExtendWith(MockitoExtension.class)
class PesquisaServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private PgVectorEmbeddingStore embeddingStore;

    @Mock
    private IDocumentoRepository documentoRepository;

    @Mock
    private CategoriaAccessService categoriaAccessService;

    @Mock
    private RagCicloVidaFiltroService ragCicloVidaFiltroService;

    private PesquisaService pesquisaService;

    @BeforeEach
    void setUp() {
        pesquisaService = new PesquisaService(
                embeddingModel, embeddingStore, documentoRepository, categoriaAccessService, ragCicloVidaFiltroService
        );

        Embedding embeddingConsulta = Embedding.from(new float[]{0.1f, 0.2f});
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(embeddingConsulta));
        lenient().when(categoriaAccessService.podeAcessarCategoria(any())).thenReturn(true);
        lenient().when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of()));
        // Default: sem pré-filtro (equivalente à flag desligada, ou metadados incompletos)
        // — cada teste que precisa de um filtro de verdade sobrescreve este stub.
        lenient().when(ragCicloVidaFiltroService.filtroStatusVigente()).thenReturn(Optional.empty());
    }

    @Test
    void deveAgruparVariosTrechosDoMesmoDocumentoEmUmUnicoResultado() {
        UUID documentoA = UUID.randomUUID();
        UUID documentoB = UUID.randomUUID();

        when(documentoRepository.findById(any(UUID.class)))
                .thenAnswer(inv -> Optional.of(documentoAtivo(inv.getArgument(0))));

        EmbeddingMatch<TextSegment> matchA1 = match(documentoA, "Documento A", "intro", "Trecho 1", 0.9);
        EmbeddingMatch<TextSegment> matchA2 = match(documentoA, "Documento A", "detalhes", "Trecho 2", 0.8);
        EmbeddingMatch<TextSegment> matchB1 = match(documentoB, "Documento B", "intro", "Trecho 3", 0.95);

        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(matchA1, matchA2, matchB1)));

        List<ResultadoBuscaDTO> resultados = pesquisaService.buscar("consulta qualquer");

        assertThat(resultados).hasSize(2);

        // Documento B tem o melhor score isolado (0.95) e deve vir primeiro.
        assertThat(resultados.get(0).documentoId()).isEqualTo(documentoB);
        assertThat(resultados.get(0).trechos()).hasSize(1);

        ResultadoBuscaDTO resultadoA = resultados.get(1);
        assertThat(resultadoA.documentoId()).isEqualTo(documentoA);
        assertThat(resultadoA.melhorScore()).isEqualTo(0.9);
        assertThat(resultadoA.trechos()).hasSize(2);
        assertThat(resultadoA.trechos().get(0).trecho()).isEqualTo("Trecho 1");
        assertThat(resultadoA.trechos().get(1).trecho()).isEqualTo("Trecho 2");
    }

    @Test
    void deveIgnorarResultadosDeDocumentosExcluidos() {
        UUID documentoExcluido = UUID.randomUUID();

        DocumentoEntity excluido = documentoAtivo(documentoExcluido);
        excluido.setDeletado(true);

        when(documentoRepository.findById(documentoExcluido)).thenReturn(Optional.of(excluido));

        EmbeddingMatch<TextSegment> match = match(documentoExcluido, "Documento Excluído", "intro", "Trecho", 0.9);

        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));

        List<ResultadoBuscaDTO> resultados = pesquisaService.buscar("consulta");

        assertThat(resultados).isEmpty();
    }

    // Etapa 16 (C4): mesma regra "vigente por padrão" do RagAssistantConfig — confere o
    // status NO BANCO (fonte da verdade), nunca no metadado do chunk.
    @Test
    void deveIgnorarResultadosDeDocumentosObsoletos() {
        UUID documentoObsoleto = UUID.randomUUID();

        DocumentoEntity obsoleto = documentoAtivo(documentoObsoleto);
        obsoleto.setStatusCicloVida(StatusCicloVida.OBSOLETO);

        when(documentoRepository.findById(documentoObsoleto)).thenReturn(Optional.of(obsoleto));

        EmbeddingMatch<TextSegment> match = match(documentoObsoleto, "Documento Obsoleto", "intro", "Trecho", 0.9);
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));

        List<ResultadoBuscaDTO> resultados = pesquisaService.buscar("consulta");

        assertThat(resultados).isEmpty();
    }

    // Documento sem status_ciclo_vida definido (legado/NAO_CLASSIFICADO) continua aparecendo
    // — "vigente por padrão" (decisão 7/C4), nunca some do RAG por falta de metadado.
    @Test
    void documentoSemStatusCicloVidaDefinidoContinuaAparecendo() {
        UUID documentoLegado = UUID.randomUUID();

        DocumentoEntity legado = documentoAtivo(documentoLegado);
        legado.setStatusCicloVida(null);

        when(documentoRepository.findById(documentoLegado)).thenReturn(Optional.of(legado));

        EmbeddingMatch<TextSegment> match = match(documentoLegado, "Documento Legado", "intro", "Trecho", 0.9);
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));

        List<ResultadoBuscaDTO> resultados = pesquisaService.buscar("consulta");

        assertThat(resultados).hasSize(1);
    }

    @Test
    void aplicaOPreFiltroQuandoRagCicloVidaFiltroServiceDevolveUm() {
        Filter filtro = MetadataFilterBuilder.metadataKey("status_ciclo_vida").isEqualTo("VIGENTE");
        when(ragCicloVidaFiltroService.filtroStatusVigente()).thenReturn(Optional.of(filtro));

        pesquisaService.buscar("consulta");

        ArgumentCaptor<EmbeddingSearchRequest> captor = ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore).search(captor.capture());
        assertThat(captor.getValue().filter()).isEqualTo(filtro);
    }

    // Mandatório (Etapa 16): com metadados incompletos (chunks legados), o pré-filtro fica
    // desligado — RagCicloVidaFiltroService.filtroStatusVigente() já devolve empty tanto com
    // a flag ligada quanto desligada (testado em RagCicloVidaFiltroServiceTest); aqui só
    // confirma que PesquisaService, ao receber empty, NUNCA anexa filtro nenhum à busca —
    // mesmo comportamento de antes da Etapa 16, nos dois estados da flag.
    @Test
    void naoAplicaFiltroNenhumQuandoRagCicloVidaFiltroServiceDevolveVazio() {
        pesquisaService.buscar("consulta");

        ArgumentCaptor<EmbeddingSearchRequest> captor = ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingStore).search(captor.capture());
        assertThat(captor.getValue().filter()).isNull();
    }

    private DocumentoEntity documentoAtivo(UUID id) {
        DocumentoEntity documento = new DocumentoEntity();
        documento.setId(id);
        documento.setDeletado(false);
        return documento;
    }

    private EmbeddingMatch<TextSegment> match(UUID documentoId, String titulo, String secao, String texto, double score) {
        Metadata metadata = new Metadata()
                .put("documento_id", documentoId)
                .put("versao", 1)
                .put("titulo", titulo)
                .put("secao", secao);

        TextSegment segment = TextSegment.from(texto, metadata);

        return new EmbeddingMatch<>(score, UUID.randomUUID().toString(), Embedding.from(new float[]{0.1f}), segment);
    }
}
