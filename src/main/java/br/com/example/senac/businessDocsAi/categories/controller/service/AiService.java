package br.com.example.senac.businessDocsAi.categories.controller.service;

import br.com.example.senac.businessDocsAi.categories.controller.entity.HistoryAction;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final HistoryService historyService;

    public AiService(HistoryService historyService) {
        this.historyService = historyService;
    }

    public void registerGeneration(Long documentId) {

        historyService.register(
                documentId,
                HistoryAction.AI_GENERATION
        );
    }
}