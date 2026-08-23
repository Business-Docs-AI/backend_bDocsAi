package br.com.example.senac.businessDocsAi.repository;

import br.com.example.senac.businessDocsAi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}