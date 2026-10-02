package br.com.example.senac.businessDocsAi.ai.ingestion;

import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexacaoServiceTest {

    @Mock
    private IDocumentoRepository documentoRepository;

    @Mock
    private HtmlSectionSplitter htmlSectionSplitter;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private PgVectorEmbeddingStore embeddingStore;

    private IndexacaoService indexacaoService;

    @BeforeEach
    void setUp() {
        indexacaoService = new IndexacaoService(documentoRepository, htmlSectionSplitter, embeddingModel, embeddingStore);
    }

    @Test
    void deveIndexarDocumentoQuandoVersaoAindaEhVigente() {
        UUID id = UUID.randomUUID();
        DocumentoEntity documento = documentoComVersao(id, 1);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(documento));
        when(htmlSectionSplitter.dividir(anyString(), anyString()))
                .thenReturn(List.of(new HtmlSectionSplitter.Secao("intro", "Introdução", "Texto de teste")));

        List<Embedding> embeddingsGerados = List.of(Embedding.from(new float[]{0.1f, 0.2f}));
        when(embeddingModel.embedAll(anyList())).thenReturn(Response.from(embeddingsGerados));

        indexacaoService.indexar(id, 1);

        verify(embeddingStore).removeAll(any(Filter.class));
        verify(embeddingStore).addAll(anyList(), eq(embeddingsGerados), anyList());
        assertThat(documento.getStatusIndexacao()).isEqualTo(StatusIndexacao.INDEXADO);
        verify(documentoRepository).save(documento);
    }

    @Test
    void indexacaoAtrasadaNaoDeveSobrescreverVersaoMaisNova() {
        UUID id = UUID.randomUUID();
        // A versão vigente já avançou para 3 quando o evento (capturado com a versão 1)
        // finalmente é processado — a indexação atrasada não pode mexer em nada.
        DocumentoEntity documento = documentoComVersao(id, 3);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(documento));

        indexacaoService.indexar(id, 1);

        verifyNoInteractions(embeddingModel);
        verify(embeddingStore, never()).removeAll(any(Filter.class));
        verify(embeddingStore, never()).addAll(anyList(), anyList(), anyList());
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void indexacaoDeveSerIgnoradaQuandoDocumentoNaoExisteMais() {
        UUID id = UUID.randomUUID();
        when(documentoRepository.findById(id)).thenReturn(Optional.empty());

        indexacaoService.indexar(id, 1);

        verifyNoInteractions(embeddingModel, embeddingStore);
    }

    @Test
    void removerEmbeddingsDeveFiltrarPorDocumentoId() {
        UUID id = UUID.randomUUID();

        indexacaoService.removerEmbeddings(id);

        verify(embeddingStore).removeAll(any(Filter.class));
    }

    @Test
    void deveMarcarStatusErroQuandoIndexacaoFalha() {
        UUID id = UUID.randomUUID();
        DocumentoEntity documento = documentoComVersao(id, 1);

        when(documentoRepository.findById(id)).thenReturn(Optional.of(documento));
        when(htmlSectionSplitter.dividir(anyString(), anyString()))
                .thenReturn(List.of(new HtmlSectionSplitter.Secao("intro", "Introdução", "Texto")));
        when(embeddingModel.embedAll(anyList())).thenThrow(new RuntimeException("Falha simulada na API"));

        indexacaoService.indexar(id, 1);

        assertThat(documento.getStatusIndexacao()).isEqualTo(StatusIndexacao.ERRO);
        verify(documentoRepository).save(documento);
    }

    private DocumentoEntity documentoComVersao(UUID id, int versaoAtual) {
        DocumentoEntity documento = new DocumentoEntity();
        documento.setId(id);
        documento.setTitulo("Documento de Teste");
        documento.setConteudoHtml("<h1>Introdução</h1><p>Texto de teste</p>");
        documento.setHashConteudo("hash");
        documento.setVersaoAtual(versaoAtual);
        documento.setStatusIndexacao(StatusIndexacao.PENDENTE);
        documento.setCriadoPor("Autor");
        documento.setCriadoEm(LocalDateTime.now());
        documento.setDeletado(false);
        return documento;
    }
}
