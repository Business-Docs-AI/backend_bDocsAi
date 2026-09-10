package br.com.example.senac.businessDocsAi.history;

import br.com.example.senac.businessDocsAi.upload.File;
import br.com.example.senac.businessDocsAi.upload.FileRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileRepository fileRepository;
    private final HistoryService historyService;

    public DocumentService(
            DocumentRepository documentRepository,
            FileRepository fileRepository,
            HistoryService historyService) {

        this.documentRepository = documentRepository;
        this.fileRepository = fileRepository;
        this.historyService = historyService;
    }

    public Document create(Document document) {

        Document savedDocument = documentRepository.save(document);

        historyService.register(
                savedDocument.getId(),
                HistoryAction.CREATE
        );

        return savedDocument;
    }

    public Document update(Long documentId, Document document) {

        Document existingDocument = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        existingDocument.setTitle(document.getTitle());

        Document updatedDocument = documentRepository.save(existingDocument);

        historyService.register(
                updatedDocument.getId(),
                HistoryAction.UPDATE
        );

        return updatedDocument;
    }

    public void delete(Long documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        documentRepository.delete(document);

        historyService.register(
                documentId,
                HistoryAction.DELETE
        );
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