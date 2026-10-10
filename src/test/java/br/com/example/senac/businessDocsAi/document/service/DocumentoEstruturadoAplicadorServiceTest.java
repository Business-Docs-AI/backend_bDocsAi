package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.dto.DocumentoEstruturadoMetadadosDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTOBeanValidationTest;
import br.com.example.senac.businessDocsAi.document.entity.Confidencialidade;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.entity.TipoDocumento;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Etapa 13.3 — decisão B3 aplicada na confirmação: o {@code conteudo_estruturado} do
 * rascunho (que pode conter TUDO, conteúdo + metadado) é separado em (a) JSON só de
 * conteúdo, passado pra versão, e (b) metadados, aplicados nas colunas do documento.
 */
@ExtendWith(MockitoExtension.class)
class DocumentoEstruturadoAplicadorServiceTest {

    @Mock
    private DocumentoService documentoService;

    private DocumentoEstruturadoAplicadorService aplicadorService;

    @BeforeEach
    void setUp() {
        aplicadorService = new DocumentoEstruturadoAplicadorService(new ObjectMapper(), documentoService);
    }

    private String jsonCompleto(DocumentoEstruturadoDTO dto) throws Exception {
        return new ObjectMapper().writeValueAsString(dto);
    }

    @Test
    void aplicarParaCriarSeparaConteudoDeMetadadoEChamaCriarComEstrutura() throws Exception {
        DocumentoEstruturadoDTO estrutura = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .macroprocessoId(5L)
                .donoProcesso("Analista Financeiro")
                .aprovador("Gestor")
                .periodicidadeRevisaoMeses(12)
                .confidencialidade(Confidencialidade.INTERNO)
                .tags(List.of("reembolso"))
                .areasParticipantes(List.of(2L, 3L))
                .build();

        RascunhoDocumentoEntity rascunho = new RascunhoDocumentoEntity();
        rascunho.setTipo(TipoRascunho.CRIAR);
        rascunho.setTitulo(estrutura.titulo());
        rascunho.setConteudoHtml("<h2>Objetivo</h2>");
        rascunho.setCategoriaId(7L);
        rascunho.setConteudoEstruturado(jsonCompleto(estrutura));
        rascunho.setStatus(StatusRascunho.GERANDO);

        DocumentoResponseDTO resultado = new DocumentoResponseDTO(
                UUID.randomUUID(), estrutura.titulo(), "<h2>Objetivo</h2>", 1, null, "Autor",
                LocalDateTime.now(), null, null, 7L, null
        );
        when(documentoService.criarComEstrutura(any(), any(), any(), any())).thenReturn(resultado);

        DocumentoResponseDTO resposta = aplicadorService.aplicar(rascunho);

        assertThat(resposta).isEqualTo(resultado);

        ArgumentCaptor<DocumentoRequestDTO> dtoCaptor = ArgumentCaptor.forClass(DocumentoRequestDTO.class);
        ArgumentCaptor<String> conteudoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> versaoCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DocumentoEstruturadoMetadadosDTO> metadadosCaptor =
                ArgumentCaptor.forClass(DocumentoEstruturadoMetadadosDTO.class);
        org.mockito.Mockito.verify(documentoService).criarComEstrutura(
                dtoCaptor.capture(), conteudoCaptor.capture(), versaoCaptor.capture(), metadadosCaptor.capture()
        );

        assertThat(dtoCaptor.getValue().titulo()).isEqualTo(estrutura.titulo());
        assertThat(dtoCaptor.getValue().categoriaId()).isEqualTo(7L);
        assertThat(versaoCaptor.getValue()).isEqualTo(DocumentoEstruturadoDTO.VERSAO_SCHEMA_ATUAL);

        // Decisão B3: o JSON de conteúdo (nível raiz) tem os campos de conteúdo...
        Map<String, Object> conteudoComoMapa = new ObjectMapper().readValue(
                conteudoCaptor.getValue(), new TypeReference<Map<String, Object>>() {
                }
        );
        assertThat(conteudoComoMapa).containsKeys("objetivo", "fluxo", "raci");
        // ...mas NUNCA os campos de metadado no nível raiz (RaciEntryDTO.aprovador é uma
        // chave de conteúdo legítima DENTRO de "raci" — "A" da matriz RACI — não deve ser
        // confundida com o metadado "aprovador" do documento, que fica fora do conteúdo).
        assertThat(conteudoComoMapa).doesNotContainKeys("donoProcesso", "aprovador", "areasParticipantes");

        DocumentoEstruturadoMetadadosDTO metadados = metadadosCaptor.getValue();
        assertThat(metadados.tipoDocumento()).isEqualTo(TipoDocumento.PROCESSO);
        assertThat(metadados.macroprocessoId()).isEqualTo(5L);
        assertThat(metadados.donoProcesso()).isEqualTo("Analista Financeiro");
        assertThat(metadados.aprovador()).isEqualTo("Gestor");
        assertThat(metadados.periodicidadeRevisaoMeses()).isEqualTo(12);
        assertThat(metadados.confidencialidade()).isEqualTo(Confidencialidade.INTERNO);
        assertThat(metadados.tags()).containsExactly("reembolso");
        assertThat(metadados.areasParticipantes()).containsExactly(2L, 3L);
    }

    @Test
    void aplicarParaAtualizarChamaAtualizarComEstruturaComODocumentoIdAlvo() throws Exception {
        DocumentoEstruturadoDTO estrutura = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder().build();
        UUID documentoIdAlvo = UUID.randomUUID();

        RascunhoDocumentoEntity rascunho = new RascunhoDocumentoEntity();
        rascunho.setTipo(TipoRascunho.ATUALIZAR);
        rascunho.setDocumentoIdAlvo(documentoIdAlvo);
        rascunho.setTitulo(estrutura.titulo());
        rascunho.setConteudoHtml("<h2>x</h2>");
        rascunho.setCategoriaId(7L);
        rascunho.setConteudoEstruturado(jsonCompleto(estrutura));
        rascunho.setStatus(StatusRascunho.GERANDO);

        DocumentoResponseDTO resultado = new DocumentoResponseDTO(
                documentoIdAlvo, estrutura.titulo(), "<h2>x</h2>", 2, null, "Autor",
                LocalDateTime.now(), "Autor", LocalDateTime.now(), 7L, null
        );
        when(documentoService.atualizarComEstrutura(any(), any(), any(), any(), any())).thenReturn(resultado);

        DocumentoResponseDTO resposta = aplicadorService.aplicar(rascunho);

        assertThat(resposta).isEqualTo(resultado);
        org.mockito.Mockito.verify(documentoService).atualizarComEstrutura(
                org.mockito.Mockito.eq(documentoIdAlvo), any(), any(), any(), any()
        );
    }

    // processoPaiId inválido (não-UUID) vindo do modelo não deve quebrar a confirmação —
    // ignora o metadado em vez de propagar uma exceção.
    @Test
    void processoPaiIdInvalidoEhIgnoradoSemFalhar() throws Exception {
        DocumentoEstruturadoDTO estrutura = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder()
                .processoPaiId("nao-e-um-uuid")
                .build();

        RascunhoDocumentoEntity rascunho = new RascunhoDocumentoEntity();
        rascunho.setTipo(TipoRascunho.CRIAR);
        rascunho.setTitulo(estrutura.titulo());
        rascunho.setConteudoHtml("<h2>x</h2>");
        rascunho.setCategoriaId(7L);
        rascunho.setConteudoEstruturado(jsonCompleto(estrutura));
        rascunho.setStatus(StatusRascunho.GERANDO);

        when(documentoService.criarComEstrutura(any(), any(), any(), any())).thenReturn(
                new DocumentoResponseDTO(
                        UUID.randomUUID(), estrutura.titulo(), "<h2>x</h2>", 1, null, "Autor",
                        LocalDateTime.now(), null, null, 7L, null
                )
        );

        ArgumentCaptor<DocumentoEstruturadoMetadadosDTO> metadadosCaptor =
                ArgumentCaptor.forClass(DocumentoEstruturadoMetadadosDTO.class);

        aplicadorService.aplicar(rascunho);

        org.mockito.Mockito.verify(documentoService).criarComEstrutura(
                any(), any(), any(), metadadosCaptor.capture()
        );
        assertThat(metadadosCaptor.getValue().processoPaiId()).isNull();
    }
}
