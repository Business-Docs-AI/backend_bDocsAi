package br.com.example.senac.businessDocsAi.categories.repository;

import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICategoryRepository extends JpaRepository<CategoryEntity, Long> {
}