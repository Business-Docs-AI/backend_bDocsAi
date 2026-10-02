package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.ai.retrieval.PesquisaService;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoVersaoResponseDTO;
import br.com.example.senac.businessDocsAi.document.dto.ResultadoBuscaDTO;
import br.com.example.senac.businessDocsAi.document.service.DocumentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;
    private final PesquisaService pesquisaService;

    @GetMapping("/busca")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ResultadoBuscaDTO>> buscar(@RequestParam("q") String query) {
        return ResponseEntity.ok(pesquisaService.buscar(query));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentoResponseDTO>> listar(
            @RequestParam(value = "categoriaId", required = false) Long categoriaId
    ) {
        return ResponseEntity.ok(documentoService.listar(categoriaId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<DocumentoResponseDTO> criar(@Valid @RequestBody DocumentoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<DocumentoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentoRequestDTO dto
    ) {
        return ResponseEntity.ok(documentoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        documentoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(documentoService.buscarPorId(id));
    }

    @GetMapping("/{id}/versoes")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<List<DocumentoVersaoResponseDTO>> listarVersoes(@PathVariable UUID id) {
        return ResponseEntity.ok(documentoService.listarVersoes(id));
    }

    @GetMapping("/{id}/versoes/{numero}")
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<DocumentoVersaoResponseDTO> buscarVersao(
            @PathVariable UUID id,
            @PathVariable int numero
    ) {
        return ResponseEntity.ok(documentoService.buscarVersao(id, numero));
    }

    @PostMapping("/{id}/versoes/{numero}/restaurar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentoResponseDTO> restaurarVersao(
            @PathVariable UUID id,
            @PathVariable int numero
    ) {
        return ResponseEntity.ok(documentoService.restaurarVersao(id, numero));
    }

    @PostMapping("/{id}/reindexar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reindexar(@PathVariable UUID id) {
        documentoService.reindexar(id);
        return ResponseEntity.noContent().build();
    }
}
