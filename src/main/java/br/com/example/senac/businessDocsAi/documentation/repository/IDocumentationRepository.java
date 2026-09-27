package br.com.example.senac.businessDocsAi.documentation.repository;

import br.com.example.senac.businessDocsAi.documentation.entity.DocumentationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDocumentationRepository extends JpaRepository<DocumentationEntity, Long> {
}