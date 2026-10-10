package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoAreaParticipanteEntity;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IDocumentoAreaParticipanteRepositoryTest {

    @Autowired
    private IDocumentoAreaParticipanteRepository repository;

    @Autowired
    private IDocumentoRepository documentoRepository;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Test
    void salvaEEncontraPorDocumentoId() {
        CategoryEntity categoria = new CategoryEntity();
        categoria.setName("Financeiro (teste área participante)");
        categoria.setDescription("desc");
        categoria = categoryRepository.save(categoria);

        DocumentoEntity documento = new DocumentoEntity();
        documento.setTitulo("Documento de teste");
        documento.setConteudoHtml("<p>x</p>");
        documento.setHashConteudo("hash-teste-area-participante");
        documento.setVersaoAtual(1);
        documento.setStatusIndexacao(StatusIndexacao.PENDENTE);
        documento.setCriadoPor("Teste");
        documento.setCriadoEm(LocalDateTime.now());
        documento.setDeletado(false);
        documento = documentoRepository.save(documento);

        repository.save(new DocumentoAreaParticipanteEntity(documento.getId(), categoria.getId()));

        List<DocumentoAreaParticipanteEntity> encontrados = repository.findByDocumentoId(documento.getId());

        assertThat(encontrados).hasSize(1);
        assertThat(encontrados.get(0).getCategoriaId()).isEqualTo(categoria.getId());
    }

    @Test
    void deleteByDocumentoIdRemoveTodosOsVinculosDoDocumento() {
        UUID documentoIdInexistenteSoParaOTeste = criarDocumentoTeste();

        CategoryEntity categoria = new CategoryEntity();
        categoria.setName("Categoria B (teste área participante)");
        categoria = categoryRepository.save(categoria);

        repository.save(new DocumentoAreaParticipanteEntity(documentoIdInexistenteSoParaOTeste, categoria.getId()));

        repository.deleteByDocumentoId(documentoIdInexistenteSoParaOTeste);

        assertThat(repository.findByDocumentoId(documentoIdInexistenteSoParaOTeste)).isEmpty();
    }

    private UUID criarDocumentoTeste() {
        DocumentoEntity documento = new DocumentoEntity();
        documento.setTitulo("Documento de teste 2");
        documento.setConteudoHtml("<p>x</p>");
        documento.setHashConteudo("hash-teste-area-participante-2");
        documento.setVersaoAtual(1);
        documento.setStatusIndexacao(StatusIndexacao.PENDENTE);
        documento.setCriadoPor("Teste");
        documento.setCriadoEm(LocalDateTime.now());
        documento.setDeletado(false);
        return documentoRepository.save(documento).getId();
    }
}
