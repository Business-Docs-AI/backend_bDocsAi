package br.com.example.senac.businessDocsAi.conversation;

import br.com.example.senac.businessDocsAi.entity.Conversation;
import br.com.example.senac.businessDocsAi.service.ConversationService;
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
    public ResponseEntity<Conversation> addFile(
            @PathVariable Long conversationId,
            @PathVariable Long fileId) {

        Conversation conversation = conversationService.addFile(
                conversationId,
                fileId
        );

        return ResponseEntity.ok(conversation);
    }
}