package br.com.example.senac.businessDocsAi.documentation.service;

import br.com.example.senac.businessDocsAi.documentation.dto.DocumentationDTO;
import br.com.example.senac.businessDocsAi.documentation.entity.Documentation;
import br.com.example.senac.businessDocsAi.documentation.repository.DocumentationRepository;
import br.com.example.senac.businessDocsAi.category.entity.Category;
import br.com.example.senac.businessDocsAi.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentationService {

    private final DocumentationRepository documentationRepository;
    private final CategoryRepository categoryRepository;


    // CREATE
    public DocumentationDTO save(DocumentationDTO dto) {

        Category category = categoryRepository
                .findById(dto.categoryId())
                .orElseThrow(() ->
                        new RuntimeException("Categoria não encontrada"));

        Documentation documentation = new Documentation();

        documentation.setTitle(dto.title());
        documentation.setContent(dto.content());
        documentation.setCategory(category);
        documentation.setCreatedBy(dto.createdBy());


        Documentation saved =
                documentationRepository.save(documentation);

        return convertToDTO(saved);
    }


    // FIND BY ID
    public DocumentationDTO findById(Long id) {

        Documentation documentation =
                documentationRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Documentação não encontrada"
                                ));

        return convertToDTO(documentation);
    }


    // FIND ALL
    public List<DocumentationDTO> findAll() {

        return documentationRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // UPDATE
    public DocumentationDTO update(Long id, DocumentationDTO dto) {

        Documentation documentation =
                documentationRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Documentação não encontrada"
                                ));


        Category category =
                categoryRepository.findById(dto.categoryId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Categoria não encontrada"
                                ));


        documentation.setTitle(dto.title());
        documentation.setContent(dto.content());
        documentation.setCategory(category);


        Documentation updated =
                documentationRepository.save(documentation);

        return convertToDTO(updated);
    }


    // DELETE
    public void deleteById(Long id) {

        if (!documentationRepository.existsById(id)) {
            throw new RuntimeException(
                    "Documentação não encontrada"
            );
        }

        documentationRepository.deleteById(id);
    }


    // CONVERTER ENTITY → DTO
    private DocumentationDTO convertToDTO(
            Documentation documentation) {

        return DocumentationDTO.builder()
                .id(documentation.getId())
                .title(documentation.getTitle())
                .content(documentation.getContent())
                .categoryId(
                        documentation.getCategory().getId()
                )
                .createdBy(documentation.getCreatedBy())
                .build();
    }
}