package br.com.example.senac.businessDocsAi.repository;

import br.com.example.senac.businessDocsAi.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}