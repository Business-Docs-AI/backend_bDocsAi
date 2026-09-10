package br.com.example.senac.businessDocsAi.history;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/documents/{documentId}")
    public List<History> listByDocument(
            @PathVariable Long documentId) {

        return historyService.listByDocument(documentId);
    }
}