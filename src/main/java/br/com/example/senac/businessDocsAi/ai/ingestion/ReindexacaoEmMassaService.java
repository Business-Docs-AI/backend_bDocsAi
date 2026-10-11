package br.com.example.senac.businessDocsAi.ai.ingestion;

import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Etapa 15: reindexação em massa — ADMIN, manual (nunca automática), assíncrona. Existe
 * principalmente para migrar chunks já indexados ANTES da Etapa 14 (sem os metadados novos
 * de pré-filtro) sem exigir que cada documento seja editado manualmente só para disparar
 * uma reindexação. Reaproveita {@link IndexacaoService#indexar} documento por documento —
 * mesma lógica de sempre (remove os chunks antigos, gera os novos, confere a versão
 * vigente), nada duplicado aqui.
 */
@Service
@RequiredArgsConstructor
public class ReindexacaoEmMassaService {

    private static final Logger log = LoggerFactory.getLogger(ReindexacaoEmMassaService.class);

    private final IDocumentoRepository documentoRepository;
    private final IndexacaoService indexacaoService;

    public int contarDocumentosAtivos() {
        return documentoRepository.findByDeletadoFalseOrderByTituloAsc().size();
    }

    @Async("reindexacaoEmMassaExecutor")
    public void reindexarTodos() {
        List<DocumentoEntity> documentos = documentoRepository.findByDeletadoFalseOrderByTituloAsc();

        log.info("Reindexação em massa iniciada: {} documento(s)", documentos.size());

        for (DocumentoEntity documento : documentos) {
            indexacaoService.indexar(documento.getId(), documento.getVersaoAtual());
        }

        log.info("Reindexação em massa concluída: {} documento(s) processados", documentos.size());
    }
}
