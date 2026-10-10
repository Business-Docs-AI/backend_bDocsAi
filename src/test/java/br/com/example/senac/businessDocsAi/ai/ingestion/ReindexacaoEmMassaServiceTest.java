package br.com.example.senac.businessDocsAi.ai.ingestion;

import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReindexacaoEmMassaServiceTest {

    @Mock
    private IDocumentoRepository documentoRepository;

    @Mock
    private IndexacaoService indexacaoService;

    private ReindexacaoEmMassaService service;

    @Test
    void reindexarTodosReindexaCadaDocumentoAtivoComASuaVersaoVigente() {
        service = new ReindexacaoEmMassaService(documentoRepository, indexacaoService);

        DocumentoEntity doc1 = documento(UUID.randomUUID(), 1);
        DocumentoEntity doc2 = documento(UUID.randomUUID(), 3);
        when(documentoRepository.findByDeletadoFalseOrderByTituloAsc()).thenReturn(List.of(doc1, doc2));

        service.reindexarTodos();

        verify(indexacaoService).indexar(doc1.getId(), 1);
        verify(indexacaoService).indexar(doc2.getId(), 3);
    }

    @Test
    void reindexarTodosSemDocumentosNaoChamaIndexacao() {
        service = new ReindexacaoEmMassaService(documentoRepository, indexacaoService);
        when(documentoRepository.findByDeletadoFalseOrderByTituloAsc()).thenReturn(List.of());

        service.reindexarTodos();

        org.mockito.Mockito.verifyNoInteractions(indexacaoService);
    }

    @Test
    void contarDocumentosAtivosDevolveOTamanhoDaListaAtual() {
        service = new ReindexacaoEmMassaService(documentoRepository, indexacaoService);
        when(documentoRepository.findByDeletadoFalseOrderByTituloAsc())
                .thenReturn(List.of(documento(UUID.randomUUID(), 1), documento(UUID.randomUUID(), 1)));

        assertThat(service.contarDocumentosAtivos()).isEqualTo(2);
    }

    private DocumentoEntity documento(UUID id, int versaoAtual) {
        DocumentoEntity documento = new DocumentoEntity();
        documento.setId(id);
        documento.setTitulo("Documento " + id);
        documento.setConteudoHtml("<p>x</p>");
        documento.setHashConteudo("hash");
        documento.setCategoriaId(1L);
        documento.setVersaoAtual(versaoAtual);
        documento.setStatusIndexacao(StatusIndexacao.INDEXADO);
        documento.setCriadoPor("Autor");
        documento.setCriadoEm(LocalDateTime.now());
        documento.setDeletado(false);
        return documento;
    }
}
