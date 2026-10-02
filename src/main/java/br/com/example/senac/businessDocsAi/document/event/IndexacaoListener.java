package br.com.example.senac.businessDocsAi.document.event;

import br.com.example.senac.businessDocsAi.ai.ingestion.IndexacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consome os eventos de documento só depois do commit da transação que os originou
 * (AFTER_COMMIT), em thread separada (@Async), para nunca segurar a transação HTTP
 * esperando a chamada de embedding.
 */
@Component
@RequiredArgsConstructor
public class IndexacaoListener {

    private final IndexacaoService indexacaoService;

    @Async("indexacaoExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoAlterarDocumento(DocumentoAlteradoEvent event) {
        indexacaoService.indexar(event.documentoId(), event.versao());
    }

    @Async("indexacaoExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoExcluirDocumento(DocumentoExcluidoEvent event) {
        indexacaoService.removerEmbeddings(event.documentoId());
    }
}
