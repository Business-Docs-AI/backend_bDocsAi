package br.com.example.senac.businessDocsAi.categories.controller;

import br.com.example.senac.businessDocsAi.categories.controller.dto.CategoryRequestDTO;
import br.com.example.senac.businessDocsAi.categories.controller.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.controller.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    // List categories
    @GetMapping
    public List<CategoryResponseDTO> list() {
        return service.list();
    }

    // Find category by ID
    @GetMapping("/{id}")
    public CategoryResponseDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    // Create category
    @PostMapping
    public CategoryResponseDTO save(@RequestBody CategoryRequestDTO dto) {
        return service.save(dto);
    }

    // Update category
    @PutMapping("/{id}")
    public CategoryResponseDTO update(
            @PathVariable Long id,
            @RequestBody CategoryRequestDTO dto) {
        return service.update(id, dto);
    }

    // Delete category
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}