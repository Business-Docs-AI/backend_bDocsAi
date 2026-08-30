package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.categories.controller.entity.Document;
import br.com.example.senac.businessDocsAi.categories.controller.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<Document> create(@RequestBody Document document) {

        Document savedDocument = documentService.create(document);

        return ResponseEntity.ok(savedDocument);
    }

    @PutMapping("/{documentId}")
    public ResponseEntity<Document> update(
            @PathVariable Long documentId,
            @RequestBody Document document) {

        Document updatedDocument = documentService.update(
                documentId,
                document
        );

        return ResponseEntity.ok(updatedDocument);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable Long documentId) {

        documentService.delete(documentId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{documentId}/files/{fileId}")
    public ResponseEntity<Document> addFile(
            @PathVariable Long documentId,
            @PathVariable Long fileId) {

        Document document = documentService.addFile(
                documentId,
                fileId
        );

        return ResponseEntity.ok(document);
    }
}