package br.com.example.senac.businessDocsAi.repository;

import br.com.example.senac.businessDocsAi.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
}