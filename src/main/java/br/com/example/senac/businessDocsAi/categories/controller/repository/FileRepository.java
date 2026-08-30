package br.com.example.senac.businessDocsAi.categories.controller.repository;

import br.com.example.senac.businessDocsAi.categories.controller.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
}