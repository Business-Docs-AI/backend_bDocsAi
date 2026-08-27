package br.com.example.senac.businessDocsAi.categories.service;

import br.com.example.senac.businessDocsAi.entity.Document;
import br.com.example.senac.businessDocsAi.entity.File;
import br.com.example.senac.businessDocsAi.repository.DocumentRepository;
import br.com.example.senac.businessDocsAi.repository.FileRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            FileRepository fileRepository) {

        this.documentRepository = documentRepository;
        this.fileRepository = fileRepository;
    }

    public Document addFile(Long documentId, Long fileId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        document.getFiles().add(file);

        return documentRepository.save(document);
    }
}