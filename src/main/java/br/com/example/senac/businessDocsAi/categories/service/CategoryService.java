package br.com.example.senac.businessDocsAi.categories.service;

import br.com.example.senac.businessDocsAi.dto.CategoryRequestDTO;
import br.com.example.senac.businessDocsAi.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.entity.Category;
import br.com.example.senac.businessDocsAi.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    // List categories
    public List<CategoryResponseDTO> list() {
        return repository.findAll()
                .stream()
                .map(category -> new CategoryResponseDTO(
                        category.getId(),
                        category.getName(),
                        category.getDescription()
                ))
                .toList();
    }

    // Find by ID
    public CategoryResponseDTO findById(Long id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }

    // Save new category
    public CategoryResponseDTO save(CategoryRequestDTO dto) {
        Category category = new Category();
        category.setName(dto.name());
        category.setDescription(dto.description());

        Category saved = repository.save(category);

        return new CategoryResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getDescription()
        );
    }

    // Update category
    public CategoryResponseDTO update(Long id, CategoryRequestDTO dto) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setName(dto.name());
        category.setDescription(dto.description());

        Category updated = repository.save(category);

        return new CategoryResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getDescription()
        );
    }

    // Delete category
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Category not found");
        }

        repository.deleteById(id);
    }
}