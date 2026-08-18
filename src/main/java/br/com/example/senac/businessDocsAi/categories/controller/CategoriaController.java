package br.com.example.senac.businessDocsAi.categories.controller;

import br.com.example.senac.businessDocsAi.dto.CategoriaRequestDTO;
import br.com.example.senac.businessDocsAi.dto.CategoriaResponseDTO;
import br.com.example.senac.businessDocsAi.service.CategoriaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    // Listar categorias
    @GetMapping
    public List<CategoriaResponseDTO> listar() {
        return service.listar();
    }

    // Buscar categoria por ID
    @GetMapping("/{id}")
    public CategoriaResponseDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    // Cadastrar categoria
    @PostMapping
    public CategoriaResponseDTO salvar(@RequestBody CategoriaRequestDTO dto) {
        return service.salvar(dto);
    }

    // Editar categoria
    @PutMapping("/{id}")
    public CategoriaResponseDTO editar(
            @PathVariable Long id,
            @RequestBody CategoriaRequestDTO dto) {
        return service.editar(id, dto);
    }

    // Excluir categoria
    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}