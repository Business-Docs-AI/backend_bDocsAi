package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.document.dto.MacroprocessoRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.MacroprocessoResponseDTO;
import br.com.example.senac.businessDocsAi.document.service.MacroprocessoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Só existe com a feature flag ligada (decisão C2c/12) — com ela desligada, esta rota
 * simplesmente não existe no sistema, igual a antes desta entrega.
 */
@RestController
@RequestMapping("/macroprocessos")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled", havingValue = "true")
public class MacroprocessoController {

    private final MacroprocessoService macroprocessoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MacroprocessoResponseDTO>> listar() {
        return ResponseEntity.ok(macroprocessoService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MacroprocessoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(macroprocessoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MacroprocessoResponseDTO> criar(@Valid @RequestBody MacroprocessoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(macroprocessoService.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MacroprocessoResponseDTO> atualizar(
            @PathVariable Long id, @Valid @RequestBody MacroprocessoRequestDTO dto
    ) {
        return ResponseEntity.ok(macroprocessoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        macroprocessoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
