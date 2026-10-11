package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.dto.MacroprocessoRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.MacroprocessoResponseDTO;
import br.com.example.senac.businessDocsAi.document.entity.MacroprocessoEntity;
import br.com.example.senac.businessDocsAi.document.repository.IMacroprocessoRepository;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CRUD simples de macroprocessos (padrão ADMIN-write / qualquer-autenticado-read, igual a
 * {@code CategoryService}) — caminho escolhido por ser o menos intrusivo (decisão C2c): sem
 * fluxo de proposta/confirmação pelo chat, que exigiria uma tabela de rascunho nova só pra
 * isso. Excluir um macroprocesso em uso por algum documento é bloqueado pela própria FK
 * (sem ON DELETE em {@code fk_documento_macroprocesso} = RESTRICT) — o
 * {@code DataIntegrityViolationException} resultante já é mapeado para 409 pelo
 * {@code GlobalExceptionHandler} existente, sem precisar de tratamento especial aqui.
 */
@Service
@RequiredArgsConstructor
public class MacroprocessoService {

    private final IMacroprocessoRepository repository;

    public List<MacroprocessoResponseDTO> listar() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public MacroprocessoResponseDTO buscarPorId(Long id) {
        return repository.findById(id)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new NotFoundException("Macroprocesso não encontrado com o ID: " + id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public MacroprocessoResponseDTO criar(MacroprocessoRequestDTO dto) {
        MacroprocessoEntity entidade = new MacroprocessoEntity();
        entidade.setNome(dto.nome());
        entidade.setDescricao(dto.descricao());

        return toResponseDTO(repository.save(entidade));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public MacroprocessoResponseDTO atualizar(Long id, MacroprocessoRequestDTO dto) {
        MacroprocessoEntity entidade = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Macroprocesso não encontrado com o ID: " + id));

        entidade.setNome(dto.nome());
        entidade.setDescricao(dto.descricao());

        return toResponseDTO(repository.save(entidade));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Macroprocesso não encontrado com o ID: " + id);
        }

        // Em uso por algum documento (fk_documento_macroprocesso, sem ON DELETE = RESTRICT)
        // → DataIntegrityViolationException, já mapeada pra 409 pelo GlobalExceptionHandler.
        repository.deleteById(id);
    }

    private MacroprocessoResponseDTO toResponseDTO(MacroprocessoEntity entidade) {
        return new MacroprocessoResponseDTO(entidade.getId(), entidade.getNome(), entidade.getDescricao());
    }
}
