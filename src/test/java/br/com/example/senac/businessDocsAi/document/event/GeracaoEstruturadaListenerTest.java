package br.com.example.senac.businessDocsAi.document.event;

import br.com.example.senac.businessDocsAi.document.service.GeracaoEstruturadaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GeracaoEstruturadaListenerTest {

    @Mock
    private GeracaoEstruturadaService geracaoEstruturadaService;

    @Test
    void aoSolicitarGeracaoChamaOWorkerComORascunhoId() {
        GeracaoEstruturadaListener listener = new GeracaoEstruturadaListener(geracaoEstruturadaService);
        UUID rascunhoId = UUID.randomUUID();

        listener.aoSolicitarGeracao(new GeracaoEstruturadaSolicitadaEvent(rascunhoId));

        verify(geracaoEstruturadaService).processar(rascunhoId);
    }
}
