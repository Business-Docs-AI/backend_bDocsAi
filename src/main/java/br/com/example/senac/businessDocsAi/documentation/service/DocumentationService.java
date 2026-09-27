package br.com.example.senac.businessDocsAi.documentation.service;

import br.com.example.senac.businessDocsAi.documentation.dto.DocumentationDTO;
import br.com.example.senac.businessDocsAi.documentation.entity.DocumentationEntity;
import br.com.example.senac.businessDocsAi.documentation.repository.IDocumentationRepository;
import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentationService {

    private final IDocumentationRepository IDocumentationRepository;
    private final ICategoryRepository ICategoryRepository;

    public DocumentationDTO save(DocumentationDTO dto) {

        CategoryEntity categoryEntity = ICategoryRepository
                .findById(dto.categoryId())
                .orElseThrow(() ->
                        new BadRequestException("Categoria não encontrada com o ID: " + dto.categoryId()));

        DocumentationEntity documentationEntity = new DocumentationEntity();

        documentationEntity.setTitle(dto.title());
        documentationEntity.setContent(dto.content());
        documentationEntity.setCategoryEntity(categoryEntity);
        documentationEntity.setCreatedBy(dto.createdBy());


        DocumentationEntity saved =
                IDocumentationRepository.save(documentationEntity);

        return convertToDTO(saved);
    }


    // FIND BY ID
    public DocumentationDTO findById(Long id) {

        DocumentationEntity documentationEntity =
                IDocumentationRepository.findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Documentação não encontrada com o ID: " + id
                                ));

        return convertToDTO(documentationEntity);
    }


    // FIND ALL
    public List<DocumentationDTO> findAll() {

        return IDocumentationRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // UPDATE
    public DocumentationDTO update(Long id, DocumentationDTO dto) {

        DocumentationEntity documentationEntity =
                IDocumentationRepository.findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Documentação não encontrada com o ID: " + id
                                ));


        CategoryEntity categoryEntity =
                ICategoryRepository.findById(dto.categoryId())
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Categoria não encontrada com o ID: " + dto.categoryId()
                                ));


        documentationEntity.setTitle(dto.title());
        documentationEntity.setContent(dto.content());
        documentationEntity.setCategoryEntity(categoryEntity);


        DocumentationEntity updated =
                IDocumentationRepository.save(documentationEntity);

        return convertToDTO(updated);
    }


    // DELETE
    public void deleteById(Long id) {

        if (!IDocumentationRepository.existsById(id)) {
            throw new NotFoundException(
                    "Documentação não encontrada com o ID: " + id
            );
        }

        IDocumentationRepository.deleteById(id);
    }


    // CONVERTER ENTITY → DTO
    private DocumentationDTO convertToDTO(
            DocumentationEntity documentationEntity) {

        return DocumentationDTO.builder()
                .id(documentationEntity.getId())
                .title(documentationEntity.getTitle())
                .content(documentationEntity.getContent())
                .categoryId(
                        documentationEntity.getCategoryEntity().getId()
                )
                .createdBy(documentationEntity.getCreatedBy())
                .build();
    }
}