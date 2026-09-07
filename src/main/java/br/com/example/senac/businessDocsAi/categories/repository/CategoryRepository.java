package br.com.example.senac.businessDocsAi.categories.repository;

import br.com.example.senac.businessDocsAi.categories.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}