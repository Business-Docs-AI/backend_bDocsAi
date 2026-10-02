package br.com.example.senac.businessDocsAi.ai.retrieval;

import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.document.dto.ResultadoBuscaDTO;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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

    private PesquisaService pesquisaService;

    @BeforeEach
    void setUp() {
        pesquisaService = new PesquisaService(embeddingModel, embeddingStore, documentoRepository, categoriaAccessService);

        Embedding embeddingConsulta = Embedding.from(new float[]{0.1f, 0.2f});
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(embeddingConsulta));
        lenient().when(categoriaAccessService.podeAcessarCategoria(any())).thenReturn(true);
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
