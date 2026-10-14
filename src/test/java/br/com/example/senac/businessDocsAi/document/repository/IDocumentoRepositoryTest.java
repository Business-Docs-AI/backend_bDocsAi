package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.categories.entity.CategoryEntity;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoryRepository;
import br.com.example.senac.businessDocsAi.document.entity.DocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code countByCategoriaId} precisa contar documentos mesmo soft-deletados (deletado=true):
 * a linha continua existindo e com ela a FK fk_documento_categoria, que bloqueia a exclusão
 * da categoria (ver {@code CategoryService.validarCategoriaSemVinculos}) mesmo que a listagem
 * normal (filtrada por deletado=false) não mostre nada vinculado.
 */
@SpringBootTest
@Transactional
class IDocumentoRepositoryTest {

    @Autowired
    private IDocumentoRepository repository;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Test
    void countByCategoriaIdContaDocumentoSoftDeletado() {
        CategoryEntity categoria = new CategoryEntity();
        categoria.setName("Categoria com documento soft-deletado");
        categoria = categoryRepository.save(categoria);

        DocumentoEntity documento = new DocumentoEntity();
        documento.setTitulo("Documento excluído");
        documento.setCategoriaId(categoria.getId());
        documento.setConteudoHtml("<p>x</p>");
        documento.setHashConteudo("hash-teste-count-categoria-id");
        documento.setVersaoAtual(1);
        documento.setStatusIndexacao(StatusIndexacao.PENDENTE);
        documento.setCriadoPor("Teste");
        documento.setCriadoEm(LocalDateTime.now());
        documento.setDeletado(true);
        repository.save(documento);

        assertThat(repository.countByCategoriaId(categoria.getId())).isEqualTo(1);
    }

    @Test
    void countByCategoriaIdEhZeroSemDocumentoVinculado() {
        CategoryEntity categoria = new CategoryEntity();
        categoria.setName("Categoria sem documentos");
        categoria = categoryRepository.save(categoria);

        assertThat(repository.countByCategoriaId(categoria.getId())).isZero();
    }
}
