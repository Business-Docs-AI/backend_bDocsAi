package br.com.example.senac.businessDocsAi.categories.controller;

import br.com.example.senac.businessDocsAi.categories.controller.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/documents/{documentId}/generate")
    public ResponseEntity<Void> registerGeneration(
            @PathVariable Long documentId) {

        aiService.registerGeneration(documentId);

        return ResponseEntity.noContent().build();
    }
}