package br.com.example.senac.businessDocsAi.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<History, Long> {

    List<History> findByDocumentId(Long documentId);
}