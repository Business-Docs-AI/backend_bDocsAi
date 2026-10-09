package br.com.example.senac.businessDocsAi.document.tool;

import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.chat.service.ConversaContextHolder;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import br.com.example.senac.businessDocsAi.document.event.GeracaoEstruturadaSolicitadaEvent;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.service.DocumentoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Etapa 13.3 — tool LEVE de solicitação de geração assíncrona. Nunca recebe o documento
 * completo; só cria/reaproveita o rascunho em GERANDO e dispara o evento pro worker.
 */
@ExtendWith(MockitoExtension.class)
class DocumentoEstruturadoToolsTest {

    @Mock
    private IRascunhoDocumentoRepository rascunhoRepository;

    @Mock
    private CategoriaAccessService categoriaAccessService;

    @Mock
    private DocumentoService documentoService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DocumentoEstruturadoTools tools;

    private final UUID conversaId = UUID.randomUUID();
    private final UUID turnoAtual = UUID.randomUUID();
    private static final Long CATEGORIA_ID = 1L;

    @BeforeEach
    void setUp() {
        tools = new DocumentoEstruturadoTools(rascunhoRepository, categoriaAccessService, documentoService, eventPublisher);
        ConversaContextHolder.iniciar(conversaId, turnoAtual);
        lenient().when(rascunhoRepository.save(any(RascunhoDocumentoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        ConversaContextHolder.limpar();
    }

    @Test
    void solicitarGeracaoCriaRascunhoEmGerandoEPublicaEvento() {
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.empty());

        String resposta = tools.solicitarGeracaoDocumentoEstruturado("Processo de Reembolso", CATEGORIA_ID, null);

        ArgumentCaptor<RascunhoDocumentoEntity> captor = ArgumentCaptor.forClass(RascunhoDocumentoEntity.class);
        verify(rascunhoRepository).save(captor.capture());

        RascunhoDocumentoEntity salvo = captor.getValue();
        assertThat(salvo.getConversaId()).isEqualTo(conversaId);
        assertThat(salvo.getTipo()).isEqualTo(TipoRascunho.CRIAR);
        assertThat(salvo.getStatus()).isEqualTo(StatusRascunho.GERANDO);
        assertThat(salvo.getConteudoHtml()).isNull();
        assertThat(salvo.getTurnoCriacao()).isEqualTo(turnoAtual);
        assertThat(salvo.getTentativasGeracao()).isZero();

        verify(eventPublisher).publishEvent(any(GeracaoEstruturadaSolicitadaEvent.class));
        assertThat(resposta).containsIgnoringCase("geração");
        verifyNoInteractions(documentoService);
    }

    @Test
    void solicitarGeracaoSemAcessoACategoriaNaoCriaRascunho() {
        doThrow(new AccessDeniedException("sem acesso")).when(categoriaAccessService).validarAcessoCategoria(anyLong());

        String resposta = tools.solicitarGeracaoDocumentoEstruturado("Titulo", CATEGORIA_ID, null);

        assertThat(resposta).containsIgnoringCase("não tem acesso");
        verifyNoInteractions(rascunhoRepository, eventPublisher);
    }

    @Test
    void solicitarGeracaoComOutroRascunhoJaGerandoNaConversaRecusa() {
        RascunhoDocumentoEntity gerando = new RascunhoDocumentoEntity();
        gerando.setStatus(StatusRascunho.GERANDO);
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(gerando));

        String resposta = tools.solicitarGeracaoDocumentoEstruturado("Outro Processo", CATEGORIA_ID, null);

        assertThat(resposta).containsIgnoringCase("já existe um documento sendo gerado");
        verify(rascunhoRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    // R1: reaproveita o rascunho existente (PENDENTE ou ERRO_GERACAO) em vez de criar um
    // segundo — mesma regra de "proposta ativa única" das tools legadas.
    @Test
    void solicitarGeracaoComRascunhoEmErroReaproveitaALinhaEPreservaOTurnoCriacao() {
        UUID turnoAnterior = UUID.randomUUID();
        RascunhoDocumentoEntity emErro = new RascunhoDocumentoEntity();
        emErro.setId(UUID.randomUUID());
        emErro.setStatus(StatusRascunho.ERRO_GERACAO);
        emErro.setErroGeracao("Falha anterior");
        emErro.setTentativasGeracao(2);
        emErro.setTurnoCriacao(turnoAnterior);
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.of(emErro));

        tools.solicitarGeracaoDocumentoEstruturado("Processo Retentativa", CATEGORIA_ID, "foco nisso");

        ArgumentCaptor<RascunhoDocumentoEntity> captor = ArgumentCaptor.forClass(RascunhoDocumentoEntity.class);
        verify(rascunhoRepository).save(captor.capture());

        RascunhoDocumentoEntity salvo = captor.getValue();
        assertThat(salvo.getId()).isEqualTo(emErro.getId());
        assertThat(salvo.getStatus()).isEqualTo(StatusRascunho.GERANDO);
        assertThat(salvo.getErroGeracao()).isNull();
        assertThat(salvo.getTentativasGeracao()).isZero();
        assertThat(salvo.getTurnoCriacao()).isEqualTo(turnoAnterior);
        assertThat(salvo.getInstrucoesAdicionais()).isEqualTo("foco nisso");
    }

    @Test
    void solicitarAtualizacaoComIdInvalidoNaoCriaRascunho() {
        String resposta = tools.solicitarAtualizacaoDocumentoEstruturado("nao-e-um-uuid", null);

        assertThat(resposta).containsIgnoringCase("inválido");
        verifyNoInteractions(rascunhoRepository, eventPublisher);
    }

    @Test
    void solicitarAtualizacaoSemAcessoAoDocumentoNaoCriaRascunho() {
        UUID documentoId = UUID.randomUUID();
        when(documentoService.buscarPorId(documentoId)).thenThrow(new AccessDeniedException("sem acesso"));

        String resposta = tools.solicitarAtualizacaoDocumentoEstruturado(documentoId.toString(), null);

        assertThat(resposta).containsIgnoringCase("não tem acesso");
        verifyNoInteractions(rascunhoRepository, eventPublisher);
    }

    @Test
    void solicitarAtualizacaoCriaRascunhoDoTipoAtualizarComOIdAlvo() {
        UUID documentoId = UUID.randomUUID();
        DocumentoResponseDTO documentoAtual = documentoResponseDTOComCategoria(documentoId, CATEGORIA_ID);
        when(documentoService.buscarPorId(documentoId)).thenReturn(documentoAtual);
        when(rascunhoRepository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(conversaId, StatusRascunho.ativos()))
                .thenReturn(Optional.empty());

        tools.solicitarAtualizacaoDocumentoEstruturado(documentoId.toString(), "instrucao");

        ArgumentCaptor<RascunhoDocumentoEntity> captor = ArgumentCaptor.forClass(RascunhoDocumentoEntity.class);
        verify(rascunhoRepository).save(captor.capture());

        RascunhoDocumentoEntity salvo = captor.getValue();
        assertThat(salvo.getTipo()).isEqualTo(TipoRascunho.ATUALIZAR);
        assertThat(salvo.getDocumentoIdAlvo()).isEqualTo(documentoId);
        assertThat(salvo.getCategoriaId()).isEqualTo(CATEGORIA_ID);
        assertThat(salvo.getStatus()).isEqualTo(StatusRascunho.GERANDO);

        verify(eventPublisher).publishEvent(any(GeracaoEstruturadaSolicitadaEvent.class));
    }

    private DocumentoResponseDTO documentoResponseDTOComCategoria(UUID id, Long categoriaId) {
        return new DocumentoResponseDTO(
                id, "Título Atual", "<p>x</p>", 1, null, "Autor", LocalDateTime.now(), null, null,
                categoriaId, "Categoria"
        );
    }
}
