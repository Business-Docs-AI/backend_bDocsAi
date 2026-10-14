package br.com.example.senac.businessDocsAi.categories.service;

import br.com.example.senac.businessDocsAi.categories.dto.CategoryRequestDTO;
import br.com.example.senac.businessDocsAi.categories.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.categories.repository.IUsuarioCategoriaRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoAreaParticipanteRepository;
import br.com.example.senac.businessDocsAi.document.repository.IDocumentoRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {

    private final ICategoryRepository repository;
    private final CategoriaAccessService categoriaAccessService;
    private final IDocumentoRepository documentoRepository;
    private final IUsuarioCategoriaRepository usuarioCategoriaRepository;
    private final IDocumentoAreaParticipanteRepository documentoAreaParticipanteRepository;

    public CategoryService(
            ICategoryRepository repository,
            CategoriaAccessService categoriaAccessService,
            IDocumentoRepository documentoRepository,
            IUsuarioCategoriaRepository usuarioCategoriaRepository,
            IDocumentoAreaParticipanteRepository documentoAreaParticipanteRepository
    ) {
        this.repository = repository;
        this.categoriaAccessService = categoriaAccessService;
        this.documentoRepository = documentoRepository;
        this.usuarioCategoriaRepository = usuarioCategoriaRepository;
        this.documentoAreaParticipanteRepository = documentoAreaParticipanteRepository;
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
    public void delete(Long id) throws NotFoundException, BadRequestException {

        if (!repository.existsById(id)) {
            throw new NotFoundException("Categoria não encontrada com o ID: " + id);
        }

        validarCategoriaSemVinculos(id);

        repository.deleteById(id);
    }

    // As FKs de documento.categoria_id, usuario_categoria.categoria_id e
    // documento_area_participante.categoria_id não têm ON DELETE CASCADE — sem essa
    // validação prévia, o delete falha com uma DataIntegrityViolationException genérica
    // (409) em vez de dizer o que está vinculado. documentoRepository.countByCategoriaId
    // conta inclusive documentos soft-deletados (deletado=true): a linha continua
    // existindo e a FK continua valendo, mesmo que não apareça em nenhuma listagem.
    private void validarCategoriaSemVinculos(Long categoriaId) {
        long totalDocumentos = documentoRepository.countByCategoriaId(categoriaId);
        long totalUsuarios = usuarioCategoriaRepository.countByCategoriaId(categoriaId);
        long totalAreasParticipantes = documentoAreaParticipanteRepository.countByCategoriaId(categoriaId);

        if (totalDocumentos == 0 && totalUsuarios == 0 && totalAreasParticipantes == 0) {
            return;
        }

        List<String> vinculos = new ArrayList<>();
        if (totalDocumentos > 0) {
            vinculos.add(totalDocumentos + " documento(s)");
        }
        if (totalUsuarios > 0) {
            vinculos.add(totalUsuarios + " usuário(s)");
        }
        if (totalAreasParticipantes > 0) {
            vinculos.add(totalAreasParticipantes + " documento(s) em que é área participante");
        }

        throw new BadRequestException(
                "Não é possível excluir esta categoria: ela ainda está vinculada a "
                        + String.join(", ", vinculos)
                        + ". Remova esses vínculos antes de excluir a categoria."
        );
    }
}