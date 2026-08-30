package br.com.example.senac.businessDocsAi.documentation.controller;

import br.com.example.senac.businessDocsAi.documentation.dto.DocumentationDTO;
import br.com.example.senac.businessDocsAi.documentation.service.DocumentationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documentations")
@RequiredArgsConstructor
public class DocumentationController {

    private final DocumentationService documentationService;


    // CREATE
    @PostMapping
    public ResponseEntity<DocumentationDTO> create(
            @RequestBody DocumentationDTO dto) {

        DocumentationDTO documentation =
                documentationService.save(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(documentation);
    }


    // FIND ALL
    @GetMapping
    public ResponseEntity<List<DocumentationDTO>> findAll() {

        List<DocumentationDTO> documentations =
                documentationService.findAll();

        return ResponseEntity.ok(documentations);
    }


    // FIND BY ID
    @GetMapping("/{id}")
    public ResponseEntity<DocumentationDTO> findById(
            @PathVariable Long id) {

        DocumentationDTO documentation =
                documentationService.findById(id);

        return ResponseEntity.ok(documentation);
    }


    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<DocumentationDTO> update(
            @PathVariable Long id,
            @RequestBody DocumentationDTO dto) {

        DocumentationDTO documentation =
                documentationService.update(id, dto);

        return ResponseEntity.ok(documentation);
    }


    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        documentationService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}