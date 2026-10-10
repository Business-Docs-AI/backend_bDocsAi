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
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Tool LEVE de solicitação de geração estruturada (Etapa 13.3) — ao contrário de {@code
 * prepararCriacaoDocumento}, NUNCA recebe o documento completo como argumento. Só cria o
 * rascunho em {@code GERANDO} e dispara o worker assíncrono (Etapa 13.4), que lê o material-
 * fonte do histórico PERSISTIDO da conversa (R4), nunca do que o modelo "repetiria" aqui.
 *
 * <p>Só existe como bean com a feature flag ligada (mesmo padrão de {@code
 * MacroprocessoTools}) — e só é adicionada à lista de tools do assistente quando o provedor
 * de chat ATIVO também está habilitado para o caminho estruturado (ver {@code
 * RagAssistantConfig} — decisão A3/Etapa 10: Gemini quebra em qualquer tool-calling via
 * AiServices, então nem a tool LEVE deveria ser oferecida nesse provedor).</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled", havingValue = "true")
public class DocumentoEstruturadoTools {

    private final IRascunhoDocumentoRepository rascunhoRepository;
    private final CategoriaAccessService categoriaAccessService;
    private final DocumentoService documentoService;
    private final ApplicationEventPublisher eventPublisher;

    @Tool("""
            Solicita a GERAÇÃO assíncrona de um documento ESTRUTURADO novo (processo ou \
            procedimento), seguindo SIPOC/RACI/fluxo por etapas — use no lugar de \
            prepararCriacaoDocumento quando o usuário pedir explicitamente um documento \
            estruturado/formal de processo. NÃO monte o documento você mesmo: a geração \
            acontece em segundo plano, a partir do histórico desta conversa. Chame \
            listarMinhasCategorias antes para saber o ID da categoria correta. Depois de \
            chamar esta ferramenta, avise o usuário de forma breve que a geração começou e \
            pode levar alguns minutos — NÃO tente montar nem mostrar o documento você \
            mesmo.""")
    public String solicitarGeracaoDocumentoEstruturado(
            @P("título sugerido para o documento") String tituloSugerido,
            @P("ID (numérico) da categoria do documento, obtido via listarMinhasCategorias") Long categoriaId,
            @P("instruções adicionais para focar a geração, se a conversa tiver outros "
                    + "assuntos além do processo a documentar — null se não houver") String instrucoesAdicionais
    ) {
        try {
            categoriaAccessService.validarAcessoCategoria(categoriaId);
        } catch (AccessDeniedException e) {
            return "Você não tem acesso a essa categoria. Chame listarMinhasCategorias para ver as categorias disponíveis.";
        }

        return solicitar(TipoRascunho.CRIAR, null, categoriaId, tituloSugerido, instrucoesAdicionais);
    }

    @Tool("""
            Solicita a GERAÇÃO assíncrona de uma ATUALIZAÇÃO estruturada de um documento já \
            existente — use no lugar de prepararAtualizacaoDocumento quando o documento \
            alvo já é (ou deveria passar a ser) um documento estruturado de processo. Use \
            buscarDocumentos antes para achar o ID certo. NÃO monte o documento você mesmo. \
            Depois de chamar esta ferramenta, avise o usuário de forma breve que a geração \
            começou.""")
    public String solicitarAtualizacaoDocumentoEstruturado(
            @P("ID (UUID) do documento a atualizar, obtido via buscarDocumentos") String documentoId,
            @P("instruções adicionais para focar a geração, se a conversa tiver outros "
                    + "assuntos além do processo a documentar — null se não houver") String instrucoesAdicionais
    ) {
        UUID id;
        try {
            id = UUID.fromString(documentoId.trim());
        } catch (IllegalArgumentException e) {
            return "ID de documento inválido: '" + documentoId + "'. Use buscarDocumentos para achar o ID correto.";
        }

        DocumentoResponseDTO documentoAtual;
        try {
            documentoAtual = documentoService.buscarPorId(id);
        } catch (AccessDeniedException e) {
            return "Você não tem acesso à categoria desse documento, então não pode propor uma atualização para ele.";
        }

        return solicitar(TipoRascunho.ATUALIZAR, id, documentoAtual.categoriaId(), documentoAtual.titulo(), instrucoesAdicionais);
    }

    private String solicitar(
            TipoRascunho tipo, UUID documentoIdAlvo, Long categoriaId, String tituloSugerido, String instrucoesAdicionais
    ) {
        var contexto = ConversaContextHolder.atual();

        // R1: mesma regra de "proposta ativa única por conversa" das tools legadas.
        Optional<RascunhoDocumentoEntity> existenteOpt = rascunhoRepository
                .findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunho.ativos());

        if (existenteOpt.isPresent() && existenteOpt.get().getStatus() == StatusRascunho.GERANDO) {
            return "Já existe um documento sendo gerado nesta conversa. Aguarde terminar, ou "
                    + "peça para descartar a proposta atual antes de pedir uma nova.";
        }

        // F2: um rascunho em ERRO_GERACAO NUNCA é reaproveitado em memória — é descartado
        // (UPDATE condicional, mesmo método/motivo do bug 3 da Etapa 13.6: nunca um save()
        // de entidade inteira) e um rascunho NOVO é criado do zero pra nova tentativa. Isso
        // preserva, como uma linha DESCARTADO própria, o histórico de cada tentativa que
        // falhou, em vez de apagar o erro anterior reescrevendo a mesma linha — e evita
        // herdar tentativasGeracao/reservadoEm de uma linha de uma geração anterior
        // completamente diferente. PENDENTE continua reaproveitado em memória (regra legada
        // de "proposta ativa única por conversa", inalterada).
        boolean haviaErroAnterior = existenteOpt.isPresent()
                && existenteOpt.get().getStatus() == StatusRascunho.ERRO_GERACAO;
        if (haviaErroAnterior) {
            rascunhoRepository.descartar(existenteOpt.get().getId(), StatusRascunho.ativos());
        }

        RascunhoDocumentoEntity rascunho = (existenteOpt.isPresent() && !haviaErroAnterior)
                ? existenteOpt.get()
                : new RascunhoDocumentoEntity();
        boolean novo = rascunho.getId() == null;

        rascunho.setConversaId(contexto.conversaId());
        rascunho.setTipo(tipo);
        rascunho.setDocumentoIdAlvo(documentoIdAlvo);
        rascunho.setCategoriaId(categoriaId);
        rascunho.setTitulo(tituloSugerido);
        rascunho.setConteudoHtml(null);
        rascunho.setConteudoEstruturado(null);
        rascunho.setVersaoSchema(null);
        rascunho.setInstrucoesAdicionais(instrucoesAdicionais);
        rascunho.setStatus(StatusRascunho.GERANDO);
        rascunho.setErroGeracao(null);
        rascunho.setTentativasGeracao(0);
        rascunho.setReservadoEm(null);
        if (novo) {
            rascunho.setTurnoCriacao(contexto.turnoAtual());
            rascunho.setCriadoEm(LocalDateTime.now());
        }

        RascunhoDocumentoEntity salvo = rascunhoRepository.save(rascunho);

        eventPublisher.publishEvent(new GeracaoEstruturadaSolicitadaEvent(salvo.getId()));

        return "A geração do documento estruturado começou e pode levar alguns minutos. "
                + "Avise o usuário brevemente que a proposta vai aparecer no painel quando estiver pronta.";
    }
}
