package br.com.example.senac.businessDocsAi.document.job;

import br.com.example.senac.businessDocsAi.ai.ingestion.IndexacaoService;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reprocessa periodicamente documentos cuja indexação ficou PENDENTE (nunca rodou) ou
 * terminou em ERRO, reaproveitando o mesmo IndexacaoService (que já confere se a versão
 * ainda é a vigente antes de indexar).
 */
@Component
@RequiredArgsConstructor
public class ReindexacaoJob {

    private static final Logger log = LoggerFactory.getLogger(ReindexacaoJob.class);

    private final IDocumentoRepository documentoRepository;
    private final IndexacaoService indexacaoService;

    @Scheduled(fixedDelayString = "${app.reindexacao.intervalo-ms:300000}")
    public void reprocessarPendentesOuComErro() {

        List<DocumentoEntity> documentos = documentoRepository
                .findByDeletadoFalseAndStatusIndexacaoIn(List.of(StatusIndexacao.PENDENTE, StatusIndexacao.ERRO));

        if (documentos.isEmpty()) {
            return;
        }

        log.info("Reprocessando {} documento(s) com indexação pendente/erro", documentos.size());

        for (DocumentoEntity documento : documentos) {
            indexacaoService.indexar(documento.getId(), documento.getVersaoAtual());
        }
    }
}
