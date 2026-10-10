package br.com.example.senac.businessDocsAi.ai.ingestion;

import br.com.example.senac.businessDocsAi.ai.retrieval.RagCicloVidaFiltroService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Etapa 15 — reindexação em massa. Só existe com a feature flag ligada (mesmo padrão de
 * {@code DocumentoStatusCicloVidaController}): sem ela, esta rota não existe, igual a antes
 * desta entrega. ADMIN-only — não é uma ação de rotina, é uma ferramenta operacional (ex.:
 * migrar chunks indexados antes da Etapa 14 para os metadados novos).
 */
@RestController
@RequestMapping("/admin/reindexacao")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled", havingValue = "true")
public class ReindexacaoEmMassaController {

    private final ReindexacaoEmMassaService reindexacaoEmMassaService;
    private final RagCicloVidaFiltroService ragCicloVidaFiltroService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReindexacaoEmMassaResponseDTO> reindexarTodos() {
        int total = reindexacaoEmMassaService.contarDocumentosAtivos();

        reindexacaoEmMassaService.reindexarTodos();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ReindexacaoEmMassaResponseDTO(
                total, "Reindexação de " + total + " documento(s) iniciada em segundo plano."
        ));
    }

    // Etapa 16 (B5, requisito opcional 4): pra saber quando a reindexação em massa já
    // terminou de corrigir os chunks órfãos — chunksSemMetadado some assim que o último
    // documento afetado for reindexado (sujeito ao cache de poucos minutos do
    // RagCicloVidaFiltroService).
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReindexacaoStatusResponseDTO> status() {
        long semMetadado = ragCicloVidaFiltroService.chunksComMetadadoIncompleto();
        boolean preFiltroAtivo = ragCicloVidaFiltroService.filtroStatusVigente().isPresent();

        return ResponseEntity.ok(new ReindexacaoStatusResponseDTO(semMetadado, preFiltroAtivo));
    }
}
