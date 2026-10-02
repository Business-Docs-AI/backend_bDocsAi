package br.com.example.senac.businessDocsAi.categories.service;

import br.com.example.senac.businessDocsAi.categories.dto.CategoryRequestDTO;
import br.com.example.senac.businessDocsAi.categories.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final ICategoryRepository repository;
    private final CategoriaAccessService categoriaAccessService;

    public CategoryService(ICategoryRepository repository, CategoriaAccessService categoriaAccessService) {
        this.repository = repository;
        this.categoriaAccessService = categoriaAccessService;
    }

    // List apenas as categorias que o usuário atual pode acessar (ADMIN vê todas).
    public List<CategoryResponseDTO> list() throws NotFoundException {
        return repository.findAll()
                .stream()
                .filter(categoryEntity -> categoriaAccessService.podeAcessarCategoria(categoryEntity.getId()))
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

        categoriaAccessService.validarAcessoCategoria(id);

        return new CategoryResponseDTO(
                categoryEntity.getId(),
                categoryEntity.getName(),
                categoryEntity.getDescription()
        );
    }

    // Save new category
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) throws NotFoundException {

        if (!repository.existsById(id)) {
            throw new NotFoundException("Categoria não encontrada com o ID: " + id);
        }

        repository.deleteById(id);
    }
}