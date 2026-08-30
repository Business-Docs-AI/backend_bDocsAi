package br.com.example.senac.businessDocsAi.categories.controller.repository;

import br.com.example.senac.businessDocsAi.categories.controller.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}