package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.chat.entity.ConversaEntity;
import br.com.example.senac.businessDocsAi.chat.repository.IConversaRepository;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Etapa 13.1 (geração assíncrona) — migração aditiva (conteudo_html nullable, colunas novas,
 * CHECK constraint) e os métodos novos de {@link IRascunhoDocumentoRepository} usados pelo
 * worker (reserva atômica e finalização condicional, R1/R3).
 */
@SpringBootTest
@Transactional
class IRascunhoDocumentoRepositoryGeracaoAssincronaTest {

    @Autowired
    private IRascunhoDocumentoRepository repository;

    @Autowired
    private IConversaRepository conversaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private UUID novaConversa() {
        ConversaEntity conversa = new ConversaEntity();
        conversa.setUsuarioId(1L);
        conversa.setTitulo("Conversa de teste (geracao assincrona)");
        conversa.setCriadoEm(LocalDateTime.now());
        return conversaRepository.save(conversa).getId();
    }

    private RascunhoDocumentoEntity novoRascunhoGerando() {
        RascunhoDocumentoEntity r = new RascunhoDocumentoEntity();
        r.setConversaId(novaConversa());
        r.setTipo(TipoRascunho.CRIAR);
        r.setTitulo("Rascunho em geração (teste)");
        r.setConteudoHtml(null);
        r.setStatus(StatusRascunho.GERANDO);
        r.setTurnoCriacao(UUID.randomUUID());
        r.setCriadoEm(LocalDateTime.now());
        r.setTentativasGeracao(0);
        RascunhoDocumentoEntity salvo = repository.save(r);
        // Flush explícito: os testes seguintes fazem UPDATE via JDBC puro/JPQL na mesma
        // transação, e precisam que o INSERT já tenha sido executado de verdade (não só
        // pendente no cache do Hibernate) para a linha existir de fato pro UPDATE achar.
        repository.flush();
        return salvo;
    }

    @Test
    void conteudoHtmlPodeSerNuloEnquantoGerando() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();

        RascunhoDocumentoEntity recarregado = repository.findById(salvo.getId()).orElseThrow();
        assertThat(recarregado.getConteudoHtml()).isNull();
        assertThat(recarregado.getStatus()).isEqualTo(StatusRascunho.GERANDO);
        assertThat(recarregado.getTentativasGeracao()).isZero();
    }

    // R6: a CHECK constraint do banco rejeita conteudo_html nulo quando o status é PENDENTE
    // ou CONFIRMADO, mesmo via SQL direto (nível de banco, não só validação da aplicação).
    @Test
    void checkConstraintRejeitaConteudoHtmlNuloQuandoPendente() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE documento_rascunho SET status = 'PENDENTE' WHERE id = ?", salvo.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraintPermitePendenteComConteudoHtmlPreenchido() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();
        salvo.setConteudoHtml("<p>conteudo</p>");
        repository.saveAndFlush(salvo);

        jdbcTemplate.update("UPDATE documento_rascunho SET status = 'PENDENTE' WHERE id = ?", salvo.getId());
        entityManager.clear();

        assertThat(repository.findById(salvo.getId()).orElseThrow().getStatus()).isEqualTo(StatusRascunho.PENDENTE);
    }

    @Test
    void reservarParaProcessamentoSoGanhaUmaVez() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime expiradoAntesDe = agora.minusMinutes(5);

        int primeiraReserva = repository.reservarParaProcessamento(salvo.getId(), agora, expiradoAntesDe, 2);
        int segundaReserva = repository.reservarParaProcessamento(salvo.getId(), agora, expiradoAntesDe, 2);

        assertThat(primeiraReserva).isEqualTo(1);
        // R3/R1: a segunda tentativa (simulando o job de segurança disputando com o
        // listener) não ganha, porque reservadoEm acabou de ser setado e ainda não expirou.
        assertThat(segundaReserva).isZero();

        RascunhoDocumentoEntity recarregado = repository.findById(salvo.getId()).orElseThrow();
        assertThat(recarregado.getTentativasGeracao()).isEqualTo(1);
        assertThat(recarregado.getReservadoEm()).isNotNull();
    }

    @Test
    void reservarParaProcessamentoFalhaQuandoTentativasEsgotadas() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();
        salvo.setTentativasGeracao(2);
        repository.save(salvo);

        int resultado = repository.reservarParaProcessamento(
                salvo.getId(), LocalDateTime.now(), LocalDateTime.now().minusMinutes(5), 2
        );

        assertThat(resultado).isZero();
    }

    @Test
    void reservarParaProcessamentoFalhaQuandoNaoEstaMaisGerando() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();
        salvo.setConteudoHtml("<p>x</p>");
        salvo.setStatus(StatusRascunho.DESCARTADO);
        repository.save(salvo);

        int resultado = repository.reservarParaProcessamento(
                salvo.getId(), LocalDateTime.now(), LocalDateTime.now().minusMinutes(5), 2
        );

        assertThat(resultado).isZero();
    }

    @Test
    void reservarParaProcessamentoGanhaDeNovoAposExpirarAReservaAnterior() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();
        LocalDateTime hamuito = LocalDateTime.now().minusMinutes(30);
        salvo.setReservadoEm(hamuito);
        repository.save(salvo);

        // job de seguranca: so considera "expirada" reserva anterior a 5 minutos atras.
        int resultado = repository.reservarParaProcessamento(
                salvo.getId(), LocalDateTime.now(), LocalDateTime.now().minusMinutes(5), 2
        );

        assertThat(resultado).isEqualTo(1);
    }

    @Test
    void finalizarComSucessoSoAplicaSeAindaEstiverGerando() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();

        int resultado = repository.finalizarComSucesso(
                salvo.getId(), "Titulo final", "<h2>Objetivo</h2>", "{\"objetivo\":\"x\"}", "1.0", 7L
        );

        assertThat(resultado).isEqualTo(1);
        RascunhoDocumentoEntity recarregado = repository.findById(salvo.getId()).orElseThrow();
        assertThat(recarregado.getStatus()).isEqualTo(StatusRascunho.PENDENTE);
        assertThat(recarregado.getConteudoHtml()).isEqualTo("<h2>Objetivo</h2>");
        // jsonb normaliza formatação (ex.: espaço após ":") no round-trip — compara
        // ignorando espaços em branco, não o byte-a-byte exato.
        assertThat(recarregado.getConteudoEstruturado()).isEqualToIgnoringWhitespace("{\"objetivo\":\"x\"}");
        assertThat(recarregado.getCategoriaId()).isEqualTo(7L);
    }

    // R1: simula o usuário descartando o rascunho ENQUANTO o worker ainda está terminando —
    // o resultado do worker não pode reviver/sobrescrever o DESCARTADO.
    @Test
    void finalizarComSucessoNaoSobrescreveRascunhoDescartadoNoMeioDaGeracao() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();

        salvo.setStatus(StatusRascunho.DESCARTADO);
        repository.save(salvo);

        int resultado = repository.finalizarComSucesso(
                salvo.getId(), "Titulo final", "<h2>Objetivo</h2>", "{\"objetivo\":\"x\"}", "1.0", 7L
        );

        assertThat(resultado).isZero();
        RascunhoDocumentoEntity recarregado = repository.findById(salvo.getId()).orElseThrow();
        assertThat(recarregado.getStatus()).isEqualTo(StatusRascunho.DESCARTADO);
        assertThat(recarregado.getConteudoHtml()).isNull();
    }

    @Test
    void finalizarComErroSoAplicaSeAindaEstiverGerando() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();

        int resultado = repository.finalizarComErro(salvo.getId(), "Falhou: tentativas esgotadas");

        assertThat(resultado).isEqualTo(1);
        RascunhoDocumentoEntity recarregado = repository.findById(salvo.getId()).orElseThrow();
        assertThat(recarregado.getStatus()).isEqualTo(StatusRascunho.ERRO_GERACAO);
        assertThat(recarregado.getErroGeracao()).isEqualTo("Falhou: tentativas esgotadas");
    }

    @Test
    void finalizarComErroNaoSobrescreveRascunhoDescartadoNoMeioDaGeracao() {
        RascunhoDocumentoEntity salvo = novoRascunhoGerando();
        salvo.setStatus(StatusRascunho.DESCARTADO);
        repository.save(salvo);

        int resultado = repository.finalizarComErro(salvo.getId(), "Falhou");

        assertThat(resultado).isZero();
        assertThat(repository.findById(salvo.getId()).orElseThrow().getStatus()).isEqualTo(StatusRascunho.DESCARTADO);
    }

    @Test
    void findFirstByConversaIdAndStatusInOrderByCriadoEmDescAcertaOMaisRecenteEntreOsAtivos() {
        UUID conversaId = novaConversa();

        RascunhoDocumentoEntity antigo = novoRascunhoAtivoNaConversa(conversaId, StatusRascunho.ERRO_GERACAO);
        antigo.setCriadoEm(LocalDateTime.now().minusMinutes(10));
        repository.save(antigo);

        RascunhoDocumentoEntity recente = novoRascunhoAtivoNaConversa(conversaId, StatusRascunho.GERANDO);
        repository.save(recente);

        var encontrado = repository.findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(
                conversaId, List.of(StatusRascunho.PENDENTE, StatusRascunho.GERANDO, StatusRascunho.ERRO_GERACAO)
        );

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(recente.getId());
    }

    private RascunhoDocumentoEntity novoRascunhoAtivoNaConversa(UUID conversaId, StatusRascunho status) {
        RascunhoDocumentoEntity r = new RascunhoDocumentoEntity();
        r.setConversaId(conversaId);
        r.setTipo(TipoRascunho.CRIAR);
        r.setTitulo("Rascunho (teste)");
        r.setConteudoHtml(status == StatusRascunho.GERANDO ? null : "<p>x</p>");
        r.setStatus(status);
        r.setTurnoCriacao(UUID.randomUUID());
        r.setCriadoEm(LocalDateTime.now());
        return r;
    }
}
