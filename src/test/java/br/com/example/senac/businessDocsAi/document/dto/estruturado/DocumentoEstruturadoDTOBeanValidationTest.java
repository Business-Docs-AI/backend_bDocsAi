package br.com.example.senac.businessDocsAi.document.dto.estruturado;

import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentoEstruturadoDTOBeanValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        factory.close();
    }

    @Test
    void fixtureValidaNaoTemViolacoes() {
        Set<ConstraintViolation<DocumentoEstruturadoDTO>> violacoes = validator.validate(fixtureValida());

        assertThat(violacoes).isEmpty();
    }

    @Test
    void objetivoEmBrancoEhRejeitado() {
        DocumentoEstruturadoDTO dto = fixtureBuilder().objetivo("").build();

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void escopoNuloEhRejeitado() {
        DocumentoEstruturadoDTO dto = fixtureBuilder().escopo(null).build();

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void fluxoVazioEhRejeitado() {
        DocumentoEstruturadoDTO dto = fixtureBuilder().fluxo(List.of()).build();

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void raciVazioEhRejeitado() {
        DocumentoEstruturadoDTO dto = fixtureBuilder().raci(List.of()).build();

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void decisaoComUmaSoOpcaoEhRejeitada() {
        EtapaDTO etapaComDecisaoRuim = new EtapaDTO(
                "E01", "Etapa", "desc", "Papel", null, List.of(), List.of(), List.of(),
                null, new DecisaoDTO("pergunta?", List.of(new DecisaoOpcaoDTO("unica opcao", "E01")))
        );
        DocumentoEstruturadoDTO dto = fixtureBuilder().fluxo(List.of(etapaComDecisaoRuim)).build();

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    @Test
    void secoesOpcionaisVaziasSaoAceitas() {
        DocumentoEstruturadoDTO dto = fixtureBuilder()
                .sipoc(null)
                .regrasNegocio(List.of())
                .excecoes(List.of())
                .sistemasFerramentas(List.of())
                .riscosControles(List.of())
                .indicadores(List.of())
                .glossario(List.of())
                .documentosRelacionados(List.of())
                .pendencias(List.of())
                .areasParticipantes(List.of())
                .tags(List.of())
                .build();

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void secaoOpcionalNulaEhRejeitada() {
        DocumentoEstruturadoDTO dto = fixtureBuilder().regrasNegocio(null).build();

        assertThat(validator.validate(dto)).isNotEmpty();
    }

    static DocumentoEstruturadoDTO fixtureValida() {
        return fixtureBuilder().build();
    }

    public static Builder fixtureBuilder() {
        EtapaDTO etapa1 = new EtapaDTO(
                "E01", "Receber pedido", "Recebe o pedido do cliente", "Atendente", null,
                List.of(), List.of(), List.of(), "E02", null
        );
        EtapaDTO etapa2 = new EtapaDTO(
                "E02", "Validar pedido", "Valida os dados do pedido", "Analista", null,
                List.of(), List.of(), List.of("RN-01"), null, null
        );

        return new Builder()
                .titulo("Processo de Teste")
                .tipoDocumento(TipoDocumento.PROCESSO)
                .objetivo("Testar o DTO")
                .escopo(new EscopoDTO("Pedido recebido", "Pedido validado", List.of(), List.of()))
                .gatilho("Cliente faz um pedido")
                .raci(List.of(new RaciEntryDTO("E01", "Atendente", "Supervisor", List.of(), List.of())))
                .fluxo(List.of(etapa1, etapa2))
                .sipoc(null)
                .regrasNegocio(List.of(new RegraNegocioDTO("RN-01", "Regra de teste", TipoRegraNegocio.CRITERIO, null)))
                .excecoes(List.of())
                .sistemasFerramentas(List.of())
                .riscosControles(List.of())
                .indicadores(List.of())
                .glossario(List.of())
                .documentosRelacionados(List.of())
                .pendencias(List.of())
                .macroprocessoId(null)
                .processoPaiId(null)
                .areasParticipantes(List.of())
                .donoProcesso(null)
                .aprovador(null)
                .periodicidadeRevisaoMeses(null)
                .confidencialidade(null)
                .tags(List.of());
    }

    /** Builder simples so pra deixar os testes legiveis - o record em si nao tem builder.
     * Publico porque DocumentoEstruturadoValidatorTest tambem reaproveita esta fixture. */
    public static class Builder {
        private String titulo;
        private TipoDocumento tipoDocumento;
        private String objetivo;
        private EscopoDTO escopo;
        private String gatilho;
        private List<RaciEntryDTO> raci;
        private List<EtapaDTO> fluxo;
        private SipocDTO sipoc;
        private List<RegraNegocioDTO> regrasNegocio;
        private List<ExcecaoDTO> excecoes;
        private List<String> sistemasFerramentas;
        private List<String> riscosControles;
        private List<String> indicadores;
        private List<GlossarioEntryDTO> glossario;
        private List<DocumentoRelacionadoDTO> documentosRelacionados;
        private List<String> pendencias;
        private Long macroprocessoId;
        private String processoPaiId;
        private List<Long> areasParticipantes;
        private String donoProcesso;
        private String aprovador;
        private Integer periodicidadeRevisaoMeses;
        private br.com.example.senac.businessDocsAi.document.entity.Confidencialidade confidencialidade;
        private List<String> tags;

        public Builder titulo(String v) { this.titulo = v; return this; }
        public Builder tipoDocumento(TipoDocumento v) { this.tipoDocumento = v; return this; }
        public Builder objetivo(String v) { this.objetivo = v; return this; }
        public Builder escopo(EscopoDTO v) { this.escopo = v; return this; }
        public Builder gatilho(String v) { this.gatilho = v; return this; }
        public Builder raci(List<RaciEntryDTO> v) { this.raci = v; return this; }
        public Builder fluxo(List<EtapaDTO> v) { this.fluxo = v; return this; }
        public Builder sipoc(SipocDTO v) { this.sipoc = v; return this; }
        public Builder regrasNegocio(List<RegraNegocioDTO> v) { this.regrasNegocio = v; return this; }
        public Builder excecoes(List<ExcecaoDTO> v) { this.excecoes = v; return this; }
        public Builder sistemasFerramentas(List<String> v) { this.sistemasFerramentas = v; return this; }
        public Builder riscosControles(List<String> v) { this.riscosControles = v; return this; }
        public Builder indicadores(List<String> v) { this.indicadores = v; return this; }
        public Builder glossario(List<GlossarioEntryDTO> v) { this.glossario = v; return this; }
        public Builder documentosRelacionados(List<DocumentoRelacionadoDTO> v) { this.documentosRelacionados = v; return this; }
        public Builder pendencias(List<String> v) { this.pendencias = v; return this; }
        public Builder macroprocessoId(Long v) { this.macroprocessoId = v; return this; }
        public Builder processoPaiId(String v) { this.processoPaiId = v; return this; }
        public Builder areasParticipantes(List<Long> v) { this.areasParticipantes = v; return this; }
        public Builder donoProcesso(String v) { this.donoProcesso = v; return this; }
        public Builder aprovador(String v) { this.aprovador = v; return this; }
        public Builder periodicidadeRevisaoMeses(Integer v) { this.periodicidadeRevisaoMeses = v; return this; }
        public Builder confidencialidade(br.com.example.senac.businessDocsAi.document.entity.Confidencialidade v) { this.confidencialidade = v; return this; }
        public Builder tags(List<String> v) { this.tags = v; return this; }

        public DocumentoEstruturadoDTO build() {
            return new DocumentoEstruturadoDTO(
                    titulo, tipoDocumento, objetivo, escopo, gatilho, raci, fluxo, sipoc, regrasNegocio,
                    excecoes, sistemasFerramentas, riscosControles, indicadores, glossario,
                    documentosRelacionados, pendencias, macroprocessoId, processoPaiId, areasParticipantes,
                    donoProcesso, aprovador, periodicidadeRevisaoMeses, confidencialidade, tags
            );
        }
    }
}
