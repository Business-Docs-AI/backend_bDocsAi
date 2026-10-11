package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.document.dto.AtualizarStatusCicloVidaRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.service.DocumentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller separado (em vez de um método novo em {@link DocumentoController}) justamente
 * para não tocar no controller existente — mesmo caminho/`DocumentoService` por baixo. Só
 * existe com a feature flag ligada (decisão 12): sem ela, esta rota não existe, igual a
 * antes desta entrega. Sem fluxo de aprovação entre os status — troca direta, decisão
 * explícita de ficar fora do escopo desta entrega.
 */
@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled", havingValue = "true")
public class DocumentoStatusCicloVidaController {

    private final DocumentoService documentoService;

    @PatchMapping("/{id}/status-ciclo-vida")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentoResponseDTO> atualizarStatusCicloVida(
            @PathVariable UUID id, @Valid @RequestBody AtualizarStatusCicloVidaRequestDTO dto
    ) {
        return ResponseEntity.ok(documentoService.atualizarStatusCicloVida(id, dto.novoStatus()));
    }
}
