package br.com.example.senac.businessDocsAi.document.tool;

import br.com.example.senac.businessDocsAi.ai.retrieval.PesquisaService;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.categories.service.CategoryService;
import br.com.example.senac.businessDocsAi.chat.service.ConversaContextHolder;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusIndexacao;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.service.DocumentoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoToolsTest {

    @Mock
    private DocumentoService documentoService;

    @Mock
    private PesquisaService pesquisaService;

    @Mock
    private IRascunhoDocumentoRepository rascunhoRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CategoriaAccessService categoriaAccessService;

    private DocumentoTools documentoTools;

    private final UUID conversaId = UUID.randomUUID();
    private final UUID turnoAtual = UUID.randomUUID();
    private static final Long CATEGORIA_ID = 1L;

    @BeforeEach
    void setUp() {
        documentoTools = new DocumentoTools(
                documentoService, pesquisaService, rascunhoRepository, categoryService, categoriaAccessService
        );
        ConversaContextHolder.iniciar(conversaId, turnoAtual);
    }

    @AfterEach
    void tearDown() {
        ConversaContextHolder.limpar();
    }

    @Test
    void prepararCriacaoDocumentoApenasGravaRascunhoSemChamarDocumentoService() {
        when(rascunhoRepository.save(any(RascunhoDocumentoEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String resposta = documentoTools.prepararCriacaoDocumento(
                "Política de Home Office", "<p>2 dias por semana</p>", CATEGORIA_ID
        );

        ArgumentCaptor<RascunhoDocumentoEntity> captor = ArgumentCaptor.forClass(RascunhoDocumentoEntity.class);
        verify(rascunhoRepository).save(captor.capture());

        RascunhoDocumentoEntity salvo = captor.getValue();
        assertThat(salvo.getConversaId()).isEqualTo(conversaId);
        assertThat(salvo.getTipo()).isEqualTo(TipoRascunho.CRIAR);
        assertThat(salvo.getStatus()).isEqualTo(StatusRascunho.PENDENTE);
        assertThat(salvo.getTurnoCriacao()).isEqualTo(turnoAtual);
        assertThat(salvo.getTitulo()).isEqualTo("Política de Home Office");

        assertThat(resposta).contains("Política de Home Office");
        verifyNoInteractions(documentoService);
    }

    @Test
    void prepararAtualizacaoDocumentoComIdInvalidoNaoGravaNada() {
        String resposta = documentoTools.prepararAtualizacaoDocumento("nao-e-um-uuid", "Título", "<p>x</p>");

        assertThat(resposta).contains("inválido");
        verifyNoInteractions(rascunhoRepository, documentoService);
    }

    @Test
    void confirmarRascunhoPendenteSemNenhumRascunhoDevolveMensagemSemChamarDocumentoService() {
        when(rascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunho.PENDENTE))
                .thenReturn(Optional.empty());

        String resposta = documentoTools.confirmarRascunhoPendente();

        assertThat(resposta).contains("Não há nenhuma proposta pendente");
        verifyNoInteractions(documentoService);
    }

    @Test
    void confirmarRascunhoPendenteNoMesmoTurnoDaPropostaEhRecusado() {
        RascunhoDocumentoEntity rascunho = rascunhoCriar(turnoAtual);

        when(rascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunho.PENDENTE))
                .thenReturn(Optional.of(rascunho));

        String resposta = documentoTools.confirmarRascunhoPendente();

        assertThat(resposta).containsIgnoringCase("ainda não é possível confirmar");
        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunho.PENDENTE);
        verifyNoInteractions(documentoService);
        verify(rascunhoRepository, never()).save(any());
    }

    @Test
    void confirmarRascunhoPendenteDeTurnoAnteriorCriaODocumentoEMarcaConfirmado() {
        UUID turnoAnterior = UUID.randomUUID();
        RascunhoDocumentoEntity rascunho = rascunhoCriar(turnoAnterior);

        when(rascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunho.PENDENTE))
                .thenReturn(Optional.of(rascunho));

        DocumentoResponseDTO documentoCriado = new DocumentoResponseDTO(
                UUID.randomUUID(), rascunho.getTitulo(), rascunho.getConteudoHtml(), 1,
                StatusIndexacao.PENDENTE, "Autor", LocalDateTime.now(), null, null, null, null
        );
        when(documentoService.criar(any())).thenReturn(documentoCriado);

        String resposta = documentoTools.confirmarRascunhoPendente();

        verify(documentoService).criar(any());
        verify(documentoService, never()).atualizar(any(), any());
        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunho.CONFIRMADO);
        assertThat(rascunho.getConfirmadoEm()).isNotNull();
        assertThat(rascunho.getDocumentoResultanteId()).isEqualTo(documentoCriado.id());
        assertThat(resposta).contains(documentoCriado.id().toString());
    }

    @Test
    void confirmarRascunhoDeAtualizacaoChamaAtualizarComODocumentoIdAlvo() {
        UUID turnoAnterior = UUID.randomUUID();
        UUID documentoIdAlvo = UUID.randomUUID();

        RascunhoDocumentoEntity rascunho = new RascunhoDocumentoEntity();
        rascunho.setConversaId(conversaId);
        rascunho.setTipo(TipoRascunho.ATUALIZAR);
        rascunho.setDocumentoIdAlvo(documentoIdAlvo);
        rascunho.setTitulo("Título Atualizado");
        rascunho.setConteudoHtml("<p>Novo conteúdo</p>");
        rascunho.setStatus(StatusRascunho.PENDENTE);
        rascunho.setTurnoCriacao(turnoAnterior);
        rascunho.setCriadoEm(LocalDateTime.now());

        when(rascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunho.PENDENTE))
                .thenReturn(Optional.of(rascunho));

        DocumentoResponseDTO documentoAtualizado = new DocumentoResponseDTO(
                documentoIdAlvo, rascunho.getTitulo(), rascunho.getConteudoHtml(), 2,
                StatusIndexacao.PENDENTE, "Autor", LocalDateTime.now(), "Autor", LocalDateTime.now(), null, null
        );
        when(documentoService.atualizar(eq(documentoIdAlvo), any())).thenReturn(documentoAtualizado);

        documentoTools.confirmarRascunhoPendente();

        verify(documentoService).atualizar(eq(documentoIdAlvo), any());
        verify(documentoService, never()).criar(any());
    }

    @Test
    void descartarRascunhoPendenteMarcaDescartadoSemChamarDocumentoService() {
        RascunhoDocumentoEntity rascunho = rascunhoCriar(UUID.randomUUID());

        when(rascunhoRepository.findFirstByConversaIdAndStatusOrderByCriadoEmDesc(conversaId, StatusRascunho.PENDENTE))
                .thenReturn(Optional.of(rascunho));

        String resposta = documentoTools.descartarRascunhoPendente();

        assertThat(rascunho.getStatus()).isEqualTo(StatusRascunho.DESCARTADO);
        assertThat(resposta).containsIgnoringCase("descartada");
        verifyNoInteractions(documentoService);
    }

    private RascunhoDocumentoEntity rascunhoCriar(UUID turno) {
        RascunhoDocumentoEntity rascunho = new RascunhoDocumentoEntity();
        rascunho.setConversaId(conversaId);
        rascunho.setTipo(TipoRascunho.CRIAR);
        rascunho.setTitulo("Política de Home Office");
        rascunho.setConteudoHtml("<p>2 dias por semana</p>");
        rascunho.setStatus(StatusRascunho.PENDENTE);
        rascunho.setTurnoCriacao(turno);
        rascunho.setCriadoEm(LocalDateTime.now());
        return rascunho;
    }
}
