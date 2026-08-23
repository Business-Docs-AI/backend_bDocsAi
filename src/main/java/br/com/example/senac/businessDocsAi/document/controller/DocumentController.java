package br.com.example.senac.businessDocsAi.document.controller;

import br.com.example.senac.businessDocsAi.entity.Document;
import br.com.example.senac.businessDocsAi.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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