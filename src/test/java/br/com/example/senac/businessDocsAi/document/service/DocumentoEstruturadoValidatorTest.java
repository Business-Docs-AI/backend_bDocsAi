package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.dto.estruturado.DecisaoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DecisaoOpcaoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTOBeanValidationTest;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.EtapaDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.RaciEntryDTO;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentoEstruturadoValidatorTest {

    private final DocumentoEstruturadoValidator validator = new DocumentoEstruturadoValidator();

    @Test
    void fixtureValidaNaoTemErros() {
        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder().build();

        assertThat(validator.validar(dto)).isEmpty();
    }

    @Test
    void tipoDocumentoNaoSuportadoNestaEntregaEhRejeitado() {
        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .tipoDocumento(TipoDocumento.POLITICA)
                .build();

        List<String> erros = validator.validar(dto);

        assertThat(erros).anyMatch(e -> e.contains("POLITICA") && e.contains("não é suportado"));
    }

    @Test
    void idsDeEtapaDuplicadosSaoRejeitados() {
        EtapaDTO e1 = etapa("E01", null, null);
        EtapaDTO e2duplicada = etapa("E01", null, null);

        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .fluxo(List.of(e1, e2duplicada))
                .raci(List.of(new RaciEntryDTO("E01", "Papel", "Papel2", List.of(), List.of())))
                .build();

        assertThat(validator.validar(dto)).anyMatch(e -> e.contains("duplicado") && e.contains("E01"));
    }

    @Test
    void etapaReferenciandoRegraInexistenteEhRejeitada() {
        EtapaDTO etapaComRegraInexistente = new EtapaDTO(
                "E01", "Nome", "desc", "Papel", null, List.of(), List.of(), List.of("RN-07"), null, null
        );

        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .fluxo(List.of(etapaComRegraInexistente))
                .raci(List.of(new RaciEntryDTO("E01", "Papel", "Papel2", List.of(), List.of())))
                .regrasNegocio(List.of())
                .build();

        List<String> erros = validator.validar(dto);

        assertThat(erros).anyMatch(e -> e.contains("E01") && e.contains("RN-07") && e.contains("não existe"));
    }

    @Test
    void decisaoComUmaSoOpcaoEhRejeitada() {
        EtapaDTO comDecisaoRuim = etapa("E01", null, new DecisaoDTO("pergunta?", List.of(new DecisaoOpcaoDTO("única", "E01"))));

        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .fluxo(List.of(comDecisaoRuim))
                .raci(List.of(new RaciEntryDTO("E01", "Papel", "Papel2", List.of(), List.of())))
                .build();

        assertThat(validator.validar(dto)).anyMatch(e -> e.contains("pelo menos 2 opções"));
    }

    @Test
    void etapaComProximaEtapaIdEDecisaoAoMesmoTempoEhRejeitada() {
        EtapaDTO ambigua = new EtapaDTO(
                "E01", "Nome", "desc", "Papel", null, List.of(), List.of(), List.of(),
                "E02", new DecisaoDTO("pergunta?", List.of(
                        new DecisaoOpcaoDTO("a", "E02"), new DecisaoOpcaoDTO("b", "E02")
                ))
        );
        EtapaDTO e2 = etapa("E02", null, null);

        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .fluxo(List.of(ambigua, e2))
                .raci(List.of(new RaciEntryDTO("E01", "Papel", "Papel2", List.of(), List.of())))
                .build();

        assertThat(validator.validar(dto)).anyMatch(e -> e.contains("E01") && e.contains("ao mesmo tempo"));
    }

    @Test
    void etapaOrfaEhRejeitada() {
        EtapaDTO e1 = etapa("E01", "E02", null);
        EtapaDTO e2 = etapa("E02", null, null);
        EtapaDTO orfa = etapa("E03", null, null); // nada aponta pra E03

        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .fluxo(List.of(e1, e2, orfa))
                .raci(List.of(new RaciEntryDTO("E01", "Papel", "Papel2", List.of(), List.of())))
                .build();

        assertThat(validator.validar(dto)).anyMatch(e -> e.contains("E03") && e.contains("órfã"));
    }

    @Test
    void raciReferenciandoEtapaInexistenteEhRejeitada() {
        EtapaDTO e1 = etapa("E01", null, null);

        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .fluxo(List.of(e1))
                .raci(List.of(new RaciEntryDTO("E99", "Papel", "Papel2", List.of(), List.of())))
                .build();

        assertThat(validator.validar(dto)).anyMatch(e -> e.contains("E99"));
    }

    private EtapaDTO etapa(String id, String proximaEtapaId, DecisaoDTO decisao) {
        return new EtapaDTO(id, "Nome " + id, "descrição", "Papel", null, List.of(), List.of(), List.of(), proximaEtapaId, decisao);
    }
}
