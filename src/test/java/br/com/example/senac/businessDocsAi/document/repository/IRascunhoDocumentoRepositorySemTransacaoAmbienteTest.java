package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.chat.entity.ConversaEntity;
import br.com.example.senac.businessDocsAi.chat.repository.IConversaRepository;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduz, sem mascarar, o bug encontrado no teste manual de ponta a ponta da Etapa 13:
 * {@code reservarParaProcessamento}/{@code finalizarComSucesso}/{@code finalizarComErro}
 * falhavam em produção com "No EntityManager with actual transaction available for current
 * thread" porque {@code GeracaoEstruturadaListener} (@Async) e {@code GeracaoEstruturadaJob}
 * (@Scheduled) não têm nenhuma transação ambiente — e o teste original
 * (IRascunhoDocumentoRepositoryGeracaoAssincronaTest) é {@code @Transactional} na própria
 * classe, então sempre teve uma transação "de graça" mascarando o problema.
 *
 * <p>Esta classe é DELIBERADAMENTE NÃO {@code @Transactional} — reproduz exatamente a
 * ausência de transação ambiente do caminho real (listener/job), confiando só no
 * {@code @Transactional} do PRÓPRIO método do repositório (a correção).</p>
 */
@SpringBootTest
class IRascunhoDocumentoRepositorySemTransacaoAmbienteTest {

    @Autowired
    private IRascunhoDocumentoRepository repository;

    @Autowired
    private IConversaRepository conversaRepository;

    private UUID rascunhoId;

    @AfterEach
    void tearDown() {
        if (rascunhoId != null) {
            repository.deleteById(rascunhoId);
        }
    }

    private UUID novaConversa() {
        ConversaEntity conversa = new ConversaEntity();
        conversa.setUsuarioId(1L);
        conversa.setTitulo("Conversa de teste (sem transacao ambiente)");
        conversa.setCriadoEm(LocalDateTime.now());
        return conversaRepository.save(conversa).getId();
    }

    private UUID novoRascunhoGerando() {
        RascunhoDocumentoEntity r = new RascunhoDocumentoEntity();
        r.setConversaId(novaConversa());
        r.setTipo(TipoRascunho.CRIAR);
        r.setTitulo("Rascunho (teste sem transacao ambiente)");
        r.setStatus(StatusRascunho.GERANDO);
        r.setTurnoCriacao(UUID.randomUUID());
        r.setCriadoEm(LocalDateTime.now());
        r.setTentativasGeracao(0);
        return repository.save(r).getId();
    }

    // Sem @Transactional na classe/método: reproduz fielmente o caminho do
    // GeracaoEstruturadaListener/GeracaoEstruturadaJob (nenhuma transação ambiente).
    @Test
    void reservarParaProcessamentoFuncionaSemTransacaoAmbiente() {
        rascunhoId = novoRascunhoGerando();

        int reservado = repository.reservarParaProcessamento(
                rascunhoId, LocalDateTime.now(), LocalDateTime.now().minusMinutes(5), 2
        );

        assertThat(reservado).isEqualTo(1);
        RascunhoDocumentoEntity recarregado = repository.findById(rascunhoId).orElseThrow();
        assertThat(recarregado.getTentativasGeracao()).isEqualTo(1);
        assertThat(recarregado.getReservadoEm()).isNotNull();
    }

    @Test
    void finalizarComSucessoFuncionaSemTransacaoAmbiente() {
        rascunhoId = novoRascunhoGerando();

        int afetados = repository.finalizarComSucesso(
                rascunhoId, "Titulo final", "<h2>x</h2>", "{\"objetivo\":\"x\"}", "1.0", 7L
        );

        assertThat(afetados).isEqualTo(1);
        assertThat(repository.findById(rascunhoId).orElseThrow().getStatus()).isEqualTo(StatusRascunho.PENDENTE);
    }

    @Test
    void finalizarComErroFuncionaSemTransacaoAmbiente() {
        rascunhoId = novoRascunhoGerando();

        int afetados = repository.finalizarComErro(rascunhoId, "Falha de teste");

        assertThat(afetados).isEqualTo(1);
        assertThat(repository.findById(rascunhoId).orElseThrow().getStatus()).isEqualTo(StatusRascunho.ERRO_GERACAO);
    }
}
