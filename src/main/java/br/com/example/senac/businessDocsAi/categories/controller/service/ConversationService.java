package br.com.example.senac.businessDocsAi.categories.controller.service;

import br.com.example.senac.businessDocsAi.categories.controller.entity.Conversation;
import br.com.example.senac.businessDocsAi.categories.controller.entity.File;
import br.com.example.senac.businessDocsAi.categories.controller.repository.ConversationRepository;
import br.com.example.senac.businessDocsAi.categories.controller.repository.FileRepository;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final FileRepository fileRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            FileRepository fileRepository) {

        this.conversationRepository = conversationRepository;
        this.fileRepository = fileRepository;
    }

    public Conversation addFile(Long conversationId, Long fileId) {

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        conversation.getFiles().add(file);

        return conversationRepository.save(conversation);
    }
}