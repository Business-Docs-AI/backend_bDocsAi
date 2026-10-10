package br.com.example.senac.businessDocsAi.document.event;

import br.com.example.senac.businessDocsAi.document.service.GeracaoEstruturadaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reação imediata à solicitação de geração estruturada (Etapa 13.4) — mesmo padrão de
 * {@code IndexacaoListener}: consome só depois do commit da transação que criou o rascunho
 * (AFTER_COMMIT), em thread separada ({@code geracaoEstruturadaExecutor}), pra nunca segurar
 * a transação HTTP original esperando a chamada de IA (que pode levar ~130s, Etapa 11b).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled", havingValue = "true")
public class GeracaoEstruturadaListener {

    private final GeracaoEstruturadaService geracaoEstruturadaService;

    @Async("geracaoEstruturadaExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoSolicitarGeracao(GeracaoEstruturadaSolicitadaEvent event) {
        geracaoEstruturadaService.processar(event.rascunhoId());
    }
}
