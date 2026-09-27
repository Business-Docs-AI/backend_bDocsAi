package br.com.example.senac.businessDocsAi.categories.service;

import br.com.example.senac.businessDocsAi.categories.dto.CategoryRequestDTO;
import br.com.example.senac.businessDocsAi.categories.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final ICategoryRepository repository;

    public CategoryService(ICategoryRepository repository) {
        this.repository = repository;
    }

    // List categories
    public List<CategoryResponseDTO> list()  throws NotFoundException {
        return repository.findAll()
                .stream()
                .map(categoryEntity -> new CategoryResponseDTO(
                        categoryEntity.getId(),
                        categoryEntity.getName(),
                        categoryEntity.getDescription()
                ))
                .toList();
    }

    // Find by ID
    public CategoryResponseDTO findById(Long id) throws NotFoundException {
        CategoryEntity categoryEntity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada com o ID: " + id));

        return new CategoryResponseDTO(
                categoryEntity.getId(),
                categoryEntity.getName(),
                categoryEntity.getDescription()
        );
    }

    // Save new category
    public CategoryResponseDTO save(CategoryRequestDTO dto) throws BadRequestException {

        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setName(dto.name());
        categoryEntity.setDescription(dto.description());

        CategoryEntity saved = repository.save(categoryEntity);

        return new CategoryResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getDescription()
        );
    }

    // Update category
    public CategoryResponseDTO update(Long id, CategoryRequestDTO dto) throws NotFoundException {
        CategoryEntity categoryEntity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada com o ID: " + id));

        categoryEntity.setName(dto.name());
        categoryEntity.setDescription(dto.description());

        CategoryEntity updated = repository.save(categoryEntity);

        return new CategoryResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getDescription()
        );
    }

    // Delete category
    public void delete(Long id) throws NotFoundException {

        if (!repository.existsById(id)) {
            throw new NotFoundException("Categoria não encontrada com o ID: " + id);
        }

        repository.deleteById(id);
    }
}