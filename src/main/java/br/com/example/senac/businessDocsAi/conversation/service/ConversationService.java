package br.com.example.senac.businessDocsAi.conversation.service;

import br.com.example.senac.businessDocsAi.conversation.entity.ConversationEntity;
import br.com.example.senac.businessDocsAi.conversation.repository.IConversationRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import br.com.example.senac.businessDocsAi.uploadFiles.entity.UploadFilesEntity;
import br.com.example.senac.businessDocsAi.uploadFiles.repository.IUploadFilesRepository;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final IConversationRepository IConversationRepository;
    private final IUploadFilesRepository uploadFilesRepository;

    public ConversationService(IConversationRepository IConversationRepository, IUploadFilesRepository uploadFilesRepository) {
        this.IConversationRepository = IConversationRepository;
        this.uploadFilesRepository = uploadFilesRepository;
    }

    public ConversationEntity addFile(Long conversationId, Long fileId) {

        ConversationEntity conversationEntity = IConversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada com o ID: " + conversationId));

        UploadFilesEntity uploadFilesEntity = uploadFilesRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado com o ID: " + fileId));

        boolean alreadyLinked = conversationEntity.getFiles().stream()
                .anyMatch(f -> f.getId().equals(uploadFilesEntity.getId()));

        if (alreadyLinked) {
            throw new BadRequestException("Este arquivo já está vinculado a esta conversa");
        }

        conversationEntity.getFiles().add(uploadFilesEntity);

        return IConversationRepository.save(conversationEntity);
    }
}
