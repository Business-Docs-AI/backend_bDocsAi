package br.com.example.senac.businessDocsAi.conversation.controller;

import br.com.example.senac.businessDocsAi.conversation.entity.ConversationEntity;
import br.com.example.senac.businessDocsAi.conversation.service.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/{conversationId}/files/{fileId}")
    public ResponseEntity<ConversationEntity> addFile(@PathVariable Long conversationId, @PathVariable Long fileId) {
        ConversationEntity conversationEntity = conversationService.addFile(
                conversationId,
                fileId
        );

        return ResponseEntity.ok(conversationEntity);
    }
}
