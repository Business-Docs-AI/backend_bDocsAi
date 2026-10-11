package br.com.example.senac.businessDocsAi.document.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirma o DEFAULT da migration V9 diretamente no banco — um INSERT bruto que não menciona
 * as colunas novas precisa receber os valores default (ex.: seeds, SQL manual). O caminho via
 * Hibernate (DocumentoService.criar) é coberto separadamente em DocumentoServiceTest, porque
 * o Hibernate manda NULL explícito para campo não setado, o que bypassaria esse DEFAULT.
 */
@SpringBootTest
@Transactional
class DocumentoMetadadosDefaultTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertBrutoSemMencionarAsColunasNovasRecebeOsDefaultsDaMigrationV9() {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO documento (id, titulo, conteudo_html, hash_conteudo, versao_atual, "
                        + "status_indexacao, criado_por, criado_em, deletado) "
                        + "VALUES (?, 'Documento de teste', '<p>x</p>', 'hash-teste', 1, "
                        + "'PENDENTE', 'Teste', now(), false)",
                id
        );

        Map<String, Object> linha = jdbcTemplate.queryForMap(
                "SELECT tipo_documento, status_ciclo_vida FROM documento WHERE id = ?", id
        );

        assertThat(linha.get("tipo_documento")).isEqualTo("NAO_CLASSIFICADO");
        assertThat(linha.get("status_ciclo_vida")).isEqualTo("VIGENTE");
    }
}
