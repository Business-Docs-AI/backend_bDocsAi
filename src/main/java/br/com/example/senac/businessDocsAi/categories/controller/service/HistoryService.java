package br.com.example.senac.businessDocsAi.categories.controller.service;

import br.com.example.senac.businessDocsAi.categories.controller.entity.History;
import br.com.example.senac.businessDocsAi.categories.controller.entity.HistoryAction;
import br.com.example.senac.businessDocsAi.categories.controller.repository.HistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoryService {

    private final HistoryRepository historyRepository;

    public HistoryService(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public History register(Long documentId, HistoryAction action) {

        History history = new History();

        history.setDocumentId(documentId);
        history.setAction(action);
        history.setCreatedAt(LocalDateTime.now());

        return historyRepository.save(history);
    }

    public List<History> listByDocument(Long documentId) {

        return historyRepository.findByDocumentId(documentId);
    }
}