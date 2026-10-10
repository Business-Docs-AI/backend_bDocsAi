package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.chat.entity.MensagemEntity;
import br.com.example.senac.businessDocsAi.chat.entity.Papel;
import br.com.example.senac.businessDocsAi.chat.repository.IMensagemRepository;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTOBeanValidationTest;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Etapa 13.4 — worker de geração assíncrona. Cobre a ORQUESTRAÇÃO (reserva, limite de
 * material, decisão retentar-vs-desistir, finalização) via um {@code spy} que substitui
 * {@link GeracaoEstruturadaService#gerarDocumentoValidado}, evitando a necessidade de um
 * {@code ChatModel} fake reproduzindo o protocolo inteiro de tool-calling do langchain4j — a
 * integração com a Anthropic de verdade (mesmo prompt/schema) já foi validada empiricamente
 * nas Etapas 10/11/11b. {@link #validarCapturado}/{@link #montarMaterial}/{@code
 * FerramentaCaptura} são testados diretamente, sem IA nenhuma.
 */
@ExtendWith(MockitoExtension.class)
class GeracaoEstruturadaServiceTest {

    @Mock
    private IRascunhoDocumentoRepository rascunhoRepository;

    @Mock
    private IMensagemRepository mensagemRepository;

    @Mock
    private EstruturaDocumentoHtmlRenderer renderer;

    @Mock
    private ChatModel chatModelGeracaoEstruturada;

    private GeracaoEstruturadaService novoService() {
        GeracaoEstruturadaService service = new GeracaoEstruturadaService(
                rascunhoRepository, mensagemRepository, new DocumentoEstruturadoValidator(),
                Validation.buildDefaultValidatorFactory().getValidator(), renderer, new ObjectMapper(),
                chatModelGeracaoEstruturada
        );
        ReflectionTestUtils.setField(service, "maxTentativas", 2);
        ReflectionTestUtils.setField(service, "timeoutGerandoMinutos", 5);
        ReflectionTestUtils.setField(service, "materialMaxCaracteres", 60_000);
        return service;
    }

    private RascunhoDocumentoEntity rascunhoGerando(UUID id, int tentativas) {
        RascunhoDocumentoEntity r = new RascunhoDocumentoEntity();
        r.setId(id);
        r.setConversaId(UUID.randomUUID());
        r.setTipo(TipoRascunho.CRIAR);
        r.setCategoriaId(7L);
        r.setTentativasGeracao(tentativas);
        r.setCriadoEm(LocalDateTime.now());
        return r;
    }

    // --- Orquestração (processar) ---

    @Test
    void processarQuandoReservaFalhaNaoFazNadaMais() {
        UUID id = UUID.randomUUID();
        when(rascunhoRepository.reservarParaProcessamento(eq(id), any(), any(), anyInt())).thenReturn(0);

        GeracaoEstruturadaService service = spy(novoService());
        service.processar(id);

        verify(rascunhoRepository, never()).findById(any());
        verifyNoInteractions(mensagemRepository, renderer);
        verify(rascunhoRepository, never()).finalizarComSucesso(any(), any(), any(), any(), any(), any());
        verify(rascunhoRepository, never()).finalizarComErro(any(), any());
    }

    @Test
    void processarComMaterialMuitoGrandeFinalizaComErroSemChamarIA() {
        UUID id = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(id, 0);
        when(rascunhoRepository.reservarParaProcessamento(eq(id), any(), any(), anyInt())).thenReturn(1);
        when(rascunhoRepository.findById(id)).thenReturn(Optional.of(rascunho));
        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(any(), any()))
                .thenReturn(List.of(mensagem(Papel.USER, "x".repeat(100))));

        GeracaoEstruturadaService service = spy(novoService());
        ReflectionTestUtils.setField(service, "materialMaxCaracteres", 10);

        service.processar(id);

        verify(rascunhoRepository).finalizarComErro(eq(id), contains("excede o limite"));
        verify(service, never()).gerarDocumentoValidado(anyString());
    }

    @Test
    void processarComSucessoRenderizaEChamaFinalizarComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(id, 0);
        when(rascunhoRepository.reservarParaProcessamento(eq(id), any(), any(), anyInt())).thenReturn(1);
        when(rascunhoRepository.findById(id)).thenReturn(Optional.of(rascunho));
        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(any(), any()))
                .thenReturn(List.of());

        DocumentoEstruturadoDTO documento = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder().build();
        when(renderer.renderizar(documento)).thenReturn("<h2>Objetivo</h2>");
        when(rascunhoRepository.finalizarComSucesso(any(), any(), any(), any(), any(), any())).thenReturn(1);

        GeracaoEstruturadaService service = spy(novoService());
        doReturn(documento).when(service).gerarDocumentoValidado(anyString());

        service.processar(id);

        verify(rascunhoRepository).finalizarComSucesso(
                eq(id), eq(documento.titulo()), eq("<h2>Objetivo</h2>"), anyString(),
                eq(DocumentoEstruturadoDTO.VERSAO_SCHEMA_ATUAL), eq(7L)
        );
        verify(rascunhoRepository, never()).finalizarComErro(any(), any());
    }

    // R1: resultado descartado em silêncio se o rascunho não estiver mais GERANDO ao terminar.
    @Test
    void processarComSucessoMasRascunhoNaoEstaMaisGerandoNaoFalha() {
        UUID id = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(id, 0);
        when(rascunhoRepository.reservarParaProcessamento(eq(id), any(), any(), anyInt())).thenReturn(1);
        when(rascunhoRepository.findById(id)).thenReturn(Optional.of(rascunho));
        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(any(), any()))
                .thenReturn(List.of());

        DocumentoEstruturadoDTO documento = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder().build();
        when(renderer.renderizar(documento)).thenReturn("<h2>x</h2>");
        when(rascunhoRepository.finalizarComSucesso(any(), any(), any(), any(), any(), any())).thenReturn(0);

        GeracaoEstruturadaService service = spy(novoService());
        doReturn(documento).when(service).gerarDocumentoValidado(anyString());

        service.processar(id); // não deve lançar nada
    }

    @Test
    void processarComDocumentoInvalidoETentativasEsgotadasFinalizaComErro() {
        UUID id = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(id, 2); // == maxTentativas (2)
        when(rascunhoRepository.reservarParaProcessamento(eq(id), any(), any(), anyInt())).thenReturn(1);
        when(rascunhoRepository.findById(id)).thenReturn(Optional.of(rascunho));
        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(any(), any()))
                .thenReturn(List.of());

        GeracaoEstruturadaService service = spy(novoService());
        doThrow(new DocumentoEstruturadoInvalidoException(List.of("E01 referencia etapa inexistente")))
                .when(service).gerarDocumentoValidado(anyString());

        service.processar(id);

        verify(rascunhoRepository).finalizarComErro(eq(id), contains("E01 referencia etapa inexistente"));
    }

    @Test
    void processarComDocumentoInvalidoETentativasRestantesNaoFinaliza() {
        UUID id = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(id, 0); // < maxTentativas (2)
        when(rascunhoRepository.reservarParaProcessamento(eq(id), any(), any(), anyInt())).thenReturn(1);
        when(rascunhoRepository.findById(id)).thenReturn(Optional.of(rascunho));
        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(any(), any()))
                .thenReturn(List.of());

        GeracaoEstruturadaService service = spy(novoService());
        doThrow(new DocumentoEstruturadoInvalidoException(List.of("erro")))
                .when(service).gerarDocumentoValidado(anyString());

        service.processar(id);

        verify(rascunhoRepository, never()).finalizarComErro(any(), any());
    }

    @Test
    void processarComExcecaoNaIAETentativasEsgotadasFinalizaComErro() {
        UUID id = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(id, 2);
        when(rascunhoRepository.reservarParaProcessamento(eq(id), any(), any(), anyInt())).thenReturn(1);
        when(rascunhoRepository.findById(id)).thenReturn(Optional.of(rascunho));
        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(any(), any()))
                .thenReturn(List.of());

        GeracaoEstruturadaService service = spy(novoService());
        doThrow(new RuntimeException("timeout simulado")).when(service).gerarDocumentoValidado(anyString());

        service.processar(id);

        verify(rascunhoRepository).finalizarComErro(eq(id), contains("Tentativas esgotadas"));
    }

    @Test
    void processarComExcecaoNaIAETentativasRestantesNaoFinaliza() {
        UUID id = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(id, 0);
        when(rascunhoRepository.reservarParaProcessamento(eq(id), any(), any(), anyInt())).thenReturn(1);
        when(rascunhoRepository.findById(id)).thenReturn(Optional.of(rascunho));
        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(any(), any()))
                .thenReturn(List.of());

        GeracaoEstruturadaService service = spy(novoService());
        doThrow(new RuntimeException("timeout simulado")).when(service).gerarDocumentoValidado(anyString());

        service.processar(id);

        verify(rascunhoRepository, never()).finalizarComErro(any(), any());
    }

    // --- Validação (sem IA) ---

    @Test
    void validarCapturadoSemChamadaDevolveErroClaro() {
        GeracaoEstruturadaService service = novoService();
        GeracaoEstruturadaService.FerramentaCaptura ferramenta = new GeracaoEstruturadaService.FerramentaCaptura();

        List<String> erros = service.validarCapturado(ferramenta);

        assertThat(erros).anyMatch(e -> e.contains("não chamou"));
    }

    @Test
    void validarCapturadoComDocumentoValidoDevolveListaVazia() {
        GeracaoEstruturadaService service = novoService();
        GeracaoEstruturadaService.FerramentaCaptura ferramenta = new GeracaoEstruturadaService.FerramentaCaptura();
        ferramenta.capturado = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder().build();

        assertThat(service.validarCapturado(ferramenta)).isEmpty();
    }

    @Test
    void validarCapturadoComViolacaoDeBeanValidationEhDetectada() {
        GeracaoEstruturadaService service = novoService();
        GeracaoEstruturadaService.FerramentaCaptura ferramenta = new GeracaoEstruturadaService.FerramentaCaptura();
        ferramenta.capturado = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder().objetivo("").build();

        assertThat(service.validarCapturado(ferramenta)).isNotEmpty();
    }

    @Test
    void ferramentaCapturaRegistraDocumentoEIncrementaChamadas() {
        GeracaoEstruturadaService.FerramentaCaptura ferramenta = new GeracaoEstruturadaService.FerramentaCaptura();
        DocumentoEstruturadoDTO dto = DocumentoEstruturadoDTOBeanValidationTest.fixtureBuilder().build();

        String resposta = ferramenta.registrarDocumentoEstruturado(dto);

        assertThat(ferramenta.capturado).isEqualTo(dto);
        assertThat(ferramenta.chamadas).isEqualTo(1);
        assertThat(resposta).isEqualTo(GeracaoEstruturadaSystemPrompt.RETORNO_FERRAMENTA_PRIMEIRA_CHAMADA);
    }

    // --- Modo modelo-fake (Etapa 13.6 — teste manual sem gastar créditos da API real) ---

    @Test
    void modeloFakeDevolveDocumentoValidoSemChamarChatModel() {
        GeracaoEstruturadaService service = novoService();
        ReflectionTestUtils.setField(service, "modeloFake", true);
        ReflectionTestUtils.setField(service, "atrasoModeloFakeMs", 0L);

        DocumentoEstruturadoDTO documento = service.gerarDocumentoValidado("material qualquer");

        assertThat(documento).isNotNull();
        assertThat(documento.titulo()).isNotBlank();
        verifyNoInteractions(chatModelGeracaoEstruturada);
    }

    @Test
    void modeloFakeDevolveDocumentoQuePassaNaValidacaoBeanEsemantica() {
        GeracaoEstruturadaService service = novoService();
        ReflectionTestUtils.setField(service, "modeloFake", true);
        ReflectionTestUtils.setField(service, "atrasoModeloFakeMs", 0L);

        GeracaoEstruturadaService.FerramentaCaptura ferramenta = new GeracaoEstruturadaService.FerramentaCaptura();
        ferramenta.capturado = service.gerarDocumentoValidado("material qualquer");

        assertThat(service.validarCapturado(ferramenta)).isEmpty();
    }

    @Test
    void processarComModeloFakeFinalizaComSucessoSemChamarChatModel() {
        UUID id = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(id, 0);
        when(rascunhoRepository.reservarParaProcessamento(eq(id), any(), any(), anyInt())).thenReturn(1);
        when(rascunhoRepository.findById(id)).thenReturn(Optional.of(rascunho));
        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(any(), any()))
                .thenReturn(List.of());
        when(renderer.renderizar(any())).thenReturn("<h2>fake</h2>");
        when(rascunhoRepository.finalizarComSucesso(any(), any(), any(), any(), any(), any())).thenReturn(1);

        GeracaoEstruturadaService service = novoService();
        ReflectionTestUtils.setField(service, "modeloFake", true);
        ReflectionTestUtils.setField(service, "atrasoModeloFakeMs", 0L);

        service.processar(id);

        verify(rascunhoRepository).finalizarComSucesso(
                eq(id), anyString(), eq("<h2>fake</h2>"), anyString(),
                eq(DocumentoEstruturadoDTO.VERSAO_SCHEMA_ATUAL), eq(7L)
        );
        verify(rascunhoRepository, never()).finalizarComErro(any(), any());
        verifyNoInteractions(chatModelGeracaoEstruturada);
    }

    // --- Material-fonte (R4) ---

    @Test
    void montarMaterialConcatenaMensagensPorPapelEInstrucoesAdicionais() {
        GeracaoEstruturadaService service = novoService();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(UUID.randomUUID(), 0);
        rascunho.setInstrucoesAdicionais("foco no processo de reembolso");

        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(
                rascunho.getConversaId(), rascunho.getCriadoEm()
        )).thenReturn(List.of(
                mensagem(Papel.USER, "pergunta do usuário"),
                mensagem(Papel.ASSISTANT, "resposta do assistente")
        ));

        String material = service.montarMaterial(rascunho);

        assertThat(material)
                .contains("Usuário: pergunta do usuário")
                .contains("Assistente: resposta do assistente")
                .contains("foco no processo de reembolso");
    }

    @Test
    void montarMaterialSemInstrucoesAdicionaisNaoAcrescentaSecaoVazia() {
        GeracaoEstruturadaService service = novoService();
        RascunhoDocumentoEntity rascunho = rascunhoGerando(UUID.randomUUID(), 0);

        when(mensagemRepository.findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(any(), any()))
                .thenReturn(List.of(mensagem(Papel.USER, "pergunta")));

        String material = service.montarMaterial(rascunho);

        assertThat(material).doesNotContain("Instruções adicionais");
    }

    private MensagemEntity mensagem(Papel papel, String conteudo) {
        MensagemEntity m = new MensagemEntity();
        m.setPapel(papel);
        m.setConteudo(conteudo);
        m.setCriadoEm(LocalDateTime.now());
        return m;
    }
}
