package br.com.example.senac.businessDocsAi.repository;

import br.com.example.senac.businessDocsAi.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}