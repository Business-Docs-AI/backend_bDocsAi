package br.com.example.senac.businessDocsAi.service;


import br.com.example.senac.businessDocsAi.dto.CategoriaRequestDTO;
import br.com.example.senac.businessDocsAi.dto.CategoriaResponseDTO;
import br.com.example.senac.businessDocsAi.entity.Categoria;
import br.com.example.senac.businessDocsAi.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    // Listar categorias
    public List<CategoriaResponseDTO> listar() {
        return repository.findAll()
                .stream()
                .map(c -> new CategoriaResponseDTO(
                        c.getId(),
                        c.getNome(),
                        c.getDescricao()
                ))
                .toList();
    }

    // Buscar por ID
    public CategoriaResponseDTO buscarPorId(Long id) {
        Categoria categoria = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao()
        );
    }

    // Salvar nova categoria
    public CategoriaResponseDTO salvar(CategoriaRequestDTO dto) {
        Categoria categoria = new Categoria();
        categoria.setNome(dto.nome());
        categoria.setDescricao(dto.descricao());

        Categoria salva = repository.save(categoria);

        return new CategoriaResponseDTO(
                salva.getId(),
                salva.getNome(),
                salva.getDescricao()
        );
    }

    // Editar categoria
    public CategoriaResponseDTO editar(Long id, CategoriaRequestDTO dto) {
        Categoria categoria = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

        categoria.setNome(dto.nome());
        categoria.setDescricao(dto.descricao());

        Categoria atualizada = repository.save(categoria);

        return new CategoriaResponseDTO(
                atualizada.getId(),
                atualizada.getNome(),
                atualizada.getDescricao()
        );
    }

    // Excluir categoria
    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Categoria não encontrada");
        }

        repository.deleteById(id);
    }
}