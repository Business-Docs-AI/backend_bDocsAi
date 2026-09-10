package br.com.example.senac.businessDocsAi.history;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico")
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentId;

    @Enumerated(EnumType.STRING)
    private HistoryAction action;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public HistoryAction getAction() {
        return action;
    }

    public void setAction(HistoryAction action) {
        this.action = action;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}