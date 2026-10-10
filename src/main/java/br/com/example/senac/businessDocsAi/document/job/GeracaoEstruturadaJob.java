package br.com.example.senac.businessDocsAi.document.job;

import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.service.GeracaoEstruturadaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rede de segurança do worker de geração estruturada (Etapa 13.4) — mesmo padrão de {@code
 * ReindexacaoJob}: varre rascunhos em GERANDO periodicamente. A reserva atômica dentro de
 * {@code GeracaoEstruturadaService.processar} (R3) garante que isso nunca reprocessa um
 * rascunho que o listener (ou outra execução deste job) já esteja processando — cobre só o
 * caso de evento perdido ou processo reiniciado no meio de uma geração.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled", havingValue = "true")
public class GeracaoEstruturadaJob {

    private static final Logger log = LoggerFactory.getLogger(GeracaoEstruturadaJob.class);

    private final IRascunhoDocumentoRepository rascunhoRepository;
    private final GeracaoEstruturadaService geracaoEstruturadaService;

    @Scheduled(fixedDelayString = "${bdocs.documentacao-estruturada.geracao.intervalo-job-ms}")
    public void reprocessarPresosEmGerando() {
        List<RascunhoDocumentoEntity> candidatos = rascunhoRepository.findByStatusOrderByCriadoEmAsc(StatusRascunho.GERANDO);

        if (candidatos.isEmpty()) {
            return;
        }

        log.info("Rede de segurança: {} rascunho(s) candidato(s) em GERANDO", candidatos.size());

        for (RascunhoDocumentoEntity candidato : candidatos) {
            geracaoEstruturadaService.processar(candidato.getId());
        }
    }
}
