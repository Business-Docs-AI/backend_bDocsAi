package br.com.example.senac.businessDocsAi.categories.controller.repository;

import br.com.example.senac.businessDocsAi.categories.controller.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}