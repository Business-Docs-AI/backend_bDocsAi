package br.com.example.senac.businessDocsAi.document.job;

import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.service.GeracaoEstruturadaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeracaoEstruturadaJobTest {

    @Mock
    private IRascunhoDocumentoRepository rascunhoRepository;

    @Mock
    private GeracaoEstruturadaService geracaoEstruturadaService;

    @Test
    void semCandidatosNaoChamaOWorker() {
        when(rascunhoRepository.findByStatusOrderByCriadoEmAsc(StatusRascunho.GERANDO)).thenReturn(List.of());

        new GeracaoEstruturadaJob(rascunhoRepository, geracaoEstruturadaService).reprocessarPresosEmGerando();

        verifyNoInteractions(geracaoEstruturadaService);
    }

    @Test
    void chamaOWorkerParaCadaCandidatoEmGerando() {
        RascunhoDocumentoEntity a = rascunho();
        RascunhoDocumentoEntity b = rascunho();
        when(rascunhoRepository.findByStatusOrderByCriadoEmAsc(StatusRascunho.GERANDO)).thenReturn(List.of(a, b));

        new GeracaoEstruturadaJob(rascunhoRepository, geracaoEstruturadaService).reprocessarPresosEmGerando();

        verify(geracaoEstruturadaService).processar(a.getId());
        verify(geracaoEstruturadaService).processar(b.getId());
    }

    private RascunhoDocumentoEntity rascunho() {
        RascunhoDocumentoEntity r = new RascunhoDocumentoEntity();
        r.setId(UUID.randomUUID());
        r.setTipo(TipoRascunho.CRIAR);
        r.setStatus(StatusRascunho.GERANDO);
        return r;
    }
}
