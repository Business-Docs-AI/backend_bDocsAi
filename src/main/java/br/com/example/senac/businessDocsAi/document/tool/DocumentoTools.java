package br.com.example.senac.businessDocsAi.document.tool;

import br.com.example.senac.businessDocsAi.ai.retrieval.PesquisaService;
import br.com.example.senac.businessDocsAi.categories.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.service.CategoriaAccessService;
import br.com.example.senac.businessDocsAi.categories.service.CategoryService;
import br.com.example.senac.businessDocsAi.chat.service.ConversaContextHolder;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.dto.ResultadoBuscaDTO;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import br.com.example.senac.businessDocsAi.document.service.DocumentoEstruturadoAplicadorService;
import br.com.example.senac.businessDocsAi.document.service.DocumentoService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Ferramentas de IA para gestão de documentos via chat. Só ficam disponíveis no assistente
 * usado por EDITOR/ADMIN (ver {@code RagAssistantConfig}) — para USUARIO essas ferramentas
 * simplesmente não existem.
 *
 * <p>Criar/atualizar de verdade (gravar em {@code documento}, versionar, indexar) NUNCA
 * acontece direto: primeiro é preciso "preparar" (grava só um rascunho, em
 * {@code documento_rascunho}) e depois "confirmar" — e confirmar só funciona se o rascunho
 * foi proposto num turno de conversa anterior ({@link ConversaContextHolder}), nunca no mesmo
 * turno em que foi proposto. Essa regra é código, não só instrução de prompt.</p>
 */
@Component
@RequiredArgsConstructor
public class DocumentoTools {

    private static final Logger log = LoggerFactory.getLogger(DocumentoTools.class);

    private final DocumentoService documentoService;
    private final PesquisaService pesquisaService;
    private final IRascunhoDocumentoRepository rascunhoRepository;
    private final CategoryService categoryService;
    private final CategoriaAccessService categoriaAccessService;
    private final DocumentoEstruturadoAplicadorService documentoEstruturadoAplicadorService;

    @Tool("""
            Lista as categorias que o usuário atual pode acessar. Use SEMPRE antes de \
            prepararCriacaoDocumento para saber em qual categoria o documento novo deve \
            entrar — se houver mais de uma, pergunte ao usuário qual delas usar; se houver \
            só uma, use-a diretamente sem perguntar.""")
    public String listarMinhasCategorias() {
        var categorias = categoryService.list();

        if (!categoriaAccessService.isAdmin()) {
            var permitidas = categoriaAccessService.categoriasDoUsuarioAtual();
            categorias = categorias.stream().filter(c -> permitidas.contains(c.id())).toList();
        }

        if (categorias.isEmpty()) {
            return "O usuário atual não está vinculado a nenhuma categoria — não é possível "
                    + "criar documentos até que um administrador vincule uma categoria a ele.";
        }

        StringBuilder texto = new StringBuilder();
        for (CategoryResponseDTO categoria : categorias) {
            texto.append("ID: ").append(categoria.id())
                    .append(" | Nome: ").append(categoria.name())
                    .append("\n");
        }
        return texto.toString();
    }

    @Tool("""
            Busca documentos de documentação já existentes por assunto/palavras-chave. Use \
            sempre antes de propor a criação de um documento novo, para checar se já não \
            existe algo equivalente (nesse caso, prefira propor uma atualização em vez de \
            criar duplicado). Também use para responder perguntas ou achar o ID de um \
            documento que o usuário quer atualizar.""")
    public String buscarDocumentos(@P("texto da busca, em linguagem natural") String consulta) {

        var resultados = pesquisaService.buscar(consulta);

        if (resultados.isEmpty()) {
            return "Nenhum documento encontrado para essa busca.";
        }

        StringBuilder texto = new StringBuilder();
        for (ResultadoBuscaDTO resultado : resultados) {
            texto.append("ID: ").append(resultado.documentoId())
                    .append(" | Título: ").append(resultado.titulo())
                    .append(" | Score: ").append(String.format("%.2f", resultado.melhorScore()))
                    .append("\n");
        }

        return texto.toString();
    }

    // Decisão ADMIN/OBSOLETO (2026-10-10): com a flag de ciclo de vida do RAG ligada,
    // buscarDocumentos (acima) passa a esconder documentos não-VIGENTES por padrão, pra
    // TODOS os papéis (inclusive ADMIN, alinhado com PesquisaService — achado na
    // verificação V2). Esta tool é o caminho EXPLÍCITO de volta pro histórico completo —
    // mesma regra de acesso por categoria de sempre, só pula o filtro de status. Com a
    // flag desligada, idêntica a buscarDocumentos (nada a pular).
    @Tool("""
            Busca documentos INCLUINDO os que não estão mais vigentes (em elaboração, em \
            revisão ou obsoletos) — use só quando o usuário pedir explicitamente por \
            histórico, versões antigas, ou documentos descontinuados/obsoletos. Para busca \
            normal, use buscarDocumentos.""")
    public String buscarDocumentosIncluindoHistorico(@P("texto da busca, em linguagem natural") String consulta) {

        var resultados = pesquisaService.buscar(consulta, true);

        if (resultados.isEmpty()) {
            return "Nenhum documento encontrado para essa busca (incluindo histórico/não-vigentes).";
        }

        StringBuilder texto = new StringBuilder();
        for (ResultadoBuscaDTO resultado : resultados) {
            texto.append("ID: ").append(resultado.documentoId())
                    .append(" | Título: ").append(resultado.titulo())
                    .append(" | Score: ").append(String.format("%.2f", resultado.melhorScore()))
                    .append("\n");
        }

        return texto.toString();
    }

    @Tool("""
            Prepara uma PROPOSTA de criação de um documento novo — NÃO cria nada ainda, só \
            grava um rascunho. Chame listarMinhasCategorias antes para saber o ID da \
            categoria correta. O sistema já exibe o título e o conteúdo completo desta \
            proposta ao usuário, formatado, numa área separada da tela — NÃO repita o \
            título nem o conteúdo HTML na sua resposta, isso só duplicaria tudo sem \
            formatação dentro da conversa. Na sua mensagem, diga de forma breve e \
            conversacional que a proposta foi preparada (cite o título e a categoria usada \
            numa frase) e pergunte se o usuário confirma. NÃO chame confirmarRascunhoPendente \
            na mesma resposta — só depois que o usuário confirmar numa mensagem separada.""")
    public String prepararCriacaoDocumento(
            @P("título proposto para o documento") String titulo,
            @P("conteúdo proposto, em HTML") String conteudoHtml,
            @P("ID (numérico) da categoria do documento, obtido via listarMinhasCategorias") Long categoriaId
    ) {
        try {
            categoriaAccessService.validarAcessoCategoria(categoriaId);
        } catch (AccessDeniedException e) {
            return "Você não tem acesso a essa categoria. Chame listarMinhasCategorias para ver as categorias disponíveis.";
        }

        var contexto = ConversaContextHolder.atual();

        // A IA às vezes "reprepara" a mesma proposta num turno seguinte (ex.: ao confirmar,
        // repete os passos de busca/preparo antes de confirmar de fato) — se isso resetasse
        // turnoCriacao, a proposta refeita nunca poderia ser confirmada no mesmo turno em
        // que foi "reproposta", mesmo já tendo passado por um turno anterior de verdade. Por
        // isso: havendo rascunho ATIVO desta conversa, atualiza o conteúdo dele mas
        // preserva o turnoCriacao original (R1 — mesma regra de sempre, agora olhando os 3
        // estados ativos, não só PENDENTE).
        RascunhoDocumentoEntity existente = rascunhoRepository
                .findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunho.ativos())
                .orElse(null);

        if (existente != null && existente.getStatus() == StatusRascunho.GERANDO) {
            return "Já existe um documento sendo gerado nesta conversa. Aguarde terminar, ou "
                    + "peça para descartar a proposta atual antes de pedir uma nova.";
        }

        RascunhoDocumentoEntity rascunho = existente != null ? existente : new RascunhoDocumentoEntity();
        boolean novo = rascunho.getId() == null;

        rascunho.setConversaId(contexto.conversaId());
        rascunho.setTipo(TipoRascunho.CRIAR);
        rascunho.setDocumentoIdAlvo(null);
        rascunho.setCategoriaId(categoriaId);
        rascunho.setTitulo(titulo);
        rascunho.setConteudoHtml(conteudoHtml);
        rascunho.setConteudoEstruturado(null);
        rascunho.setVersaoSchema(null);
        rascunho.setStatus(StatusRascunho.PENDENTE);
        rascunho.setErroGeracao(null);
        rascunho.setTentativasGeracao(0);
        if (novo) {
            rascunho.setTurnoCriacao(contexto.turnoAtual());
            rascunho.setCriadoEm(LocalDateTime.now());
        }

        rascunhoRepository.save(rascunho);

        return """
                Rascunho de CRIAÇÃO preparado (ainda não salvo). O sistema já vai exibir o \
                título e o conteúdo completo ao usuário, formatado, numa área separada da \
                tela — NÃO repita o conteúdo HTML na sua próxima mensagem. Apenas avise \
                brevemente que a proposta foi preparada (pode citar o título "%s" e a \
                categoria usada) e peça a confirmação do usuário.""".formatted(titulo);
    }

    @Tool("""
            Prepara uma PROPOSTA de atualização de um documento já existente — NÃO atualiza \
            nada ainda, só grava um rascunho. Use buscarDocumentos antes para achar o ID \
            certo. O sistema já exibe o novo título e o novo conteúdo completo desta \
            proposta ao usuário, formatado, numa área separada da tela — NÃO repita o \
            título nem o conteúdo HTML na sua resposta. Na sua mensagem, diga de forma \
            breve e conversacional que a proposta de atualização foi preparada e pergunte \
            se o usuário confirma. NÃO chame confirmarRascunhoPendente na mesma resposta — \
            só depois que o usuário confirmar numa mensagem separada.""")
    public String prepararAtualizacaoDocumento(
            @P("ID (UUID) do documento a atualizar, obtido via buscarDocumentos") String documentoId,
            @P("novo título proposto") String titulo,
            @P("novo conteúdo proposto, em HTML, completo (substitui o anterior)") String conteudoHtml
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

        var contexto = ConversaContextHolder.atual();

        // Ver prepararCriacaoDocumento: preserva o turnoCriacao de um rascunho ATIVO já
        // existente desta conversa em vez de resetá-lo a cada "reproposta" (R1).
        RascunhoDocumentoEntity existente = rascunhoRepository
                .findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunho.ativos())
                .orElse(null);

        if (existente != null && existente.getStatus() == StatusRascunho.GERANDO) {
            return "Já existe um documento sendo gerado nesta conversa. Aguarde terminar, ou "
                    + "peça para descartar a proposta atual antes de pedir uma nova.";
        }

        RascunhoDocumentoEntity rascunho = existente != null ? existente : new RascunhoDocumentoEntity();
        boolean novo = rascunho.getId() == null;

        rascunho.setConversaId(contexto.conversaId());
        rascunho.setTipo(TipoRascunho.ATUALIZAR);
        rascunho.setDocumentoIdAlvo(id);
        rascunho.setCategoriaId(documentoAtual.categoriaId());
        rascunho.setTitulo(titulo);
        rascunho.setConteudoHtml(conteudoHtml);
        rascunho.setConteudoEstruturado(null);
        rascunho.setVersaoSchema(null);
        rascunho.setStatus(StatusRascunho.PENDENTE);
        rascunho.setErroGeracao(null);
        rascunho.setTentativasGeracao(0);
        if (novo) {
            rascunho.setTurnoCriacao(contexto.turnoAtual());
            rascunho.setCriadoEm(LocalDateTime.now());
        }

        rascunhoRepository.save(rascunho);

        return """
                Rascunho de ATUALIZAÇÃO do documento %s preparado (ainda não salvo). O \
                sistema já vai exibir o novo título e o novo conteúdo completo ao usuário, \
                formatado, numa área separada da tela — NÃO repita o conteúdo HTML na sua \
                próxima mensagem. Apenas avise brevemente que a proposta de atualização \
                (novo título: "%s") foi preparada e peça a confirmação do usuário.""".formatted(id, titulo);
    }

    @Tool("""
            Confirma e efetiva a última proposta de criação/atualização de documento feita \
            nesta conversa (a que foi preparada com prepararCriacaoDocumento ou \
            prepararAtualizacaoDocumento). SÓ chame isto depois que o usuário confirmar \
            explicitamente, em uma mensagem à parte, que quer seguir com o que foi \
            proposto — nunca no mesmo turno em que a proposta foi feita.""")
    public String confirmarRascunhoPendente() {

        var contexto = ConversaContextHolder.atual();

        Optional<RascunhoDocumentoEntity> rascunhoOpt = rascunhoRepository
                .findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunho.ativos());

        if (rascunhoOpt.isEmpty()) {
            return "Não há nenhuma proposta pendente para confirmar nesta conversa.";
        }

        RascunhoDocumentoEntity rascunho = rascunhoOpt.get();

        // R1: geração assíncrona em andamento ou que falhou nunca pode ser confirmada.
        if (rascunho.getStatus() == StatusRascunho.GERANDO) {
            return "O documento ainda está sendo gerado — aguarde terminar antes de confirmar.";
        }
        if (rascunho.getStatus() == StatusRascunho.ERRO_GERACAO) {
            return "A geração deste documento falhou (" + rascunho.getErroGeracao() + "). Peça "
                    + "para gerar de novo ou descarte a proposta.";
        }

        if (rascunho.getTurnoCriacao().equals(contexto.turnoAtual())) {
            log.warn(
                    "Tentativa de confirmar rascunho {} no mesmo turno em que foi proposto (conversa {})",
                    rascunho.getId(), contexto.conversaId()
            );
            return "Ainda não é possível confirmar: a proposta precisa ser mostrada ao usuário "
                    + "e confirmada em uma mensagem separada antes de ser efetivada.";
        }

        try {
            categoriaAccessService.validarAcessoCategoria(rascunho.getCategoriaId());
        } catch (AccessDeniedException e) {
            return "Você não tem mais acesso à categoria dessa proposta — ela não pôde ser confirmada.";
        }

        // Decisão B3: um rascunho com conteudoEstruturado preenchido (veio do worker de
        // geração assíncrona, Etapa 13.4) confirma pelo caminho que separa conteúdo de
        // metadado; o legado (prepararCriacaoDocumento/...Atualizacao...) nunca preenche
        // esse campo, então sempre cai no caminho de hoje, sem nenhuma mudança de
        // comportamento para ele.
        DocumentoResponseDTO documento;
        if (rascunho.getConteudoEstruturado() != null) {
            documento = documentoEstruturadoAplicadorService.aplicar(rascunho);
        } else {
            DocumentoRequestDTO dto = new DocumentoRequestDTO(
                    rascunho.getTitulo(), rascunho.getConteudoHtml(), "Criado/atualizado via chat com IA",
                    rascunho.getCategoriaId()
            );

            documento = rascunho.getTipo() == TipoRascunho.CRIAR
                    ? documentoService.criar(dto)
                    : documentoService.atualizar(rascunho.getDocumentoIdAlvo(), dto);
        }

        rascunho.setStatus(StatusRascunho.CONFIRMADO);
        rascunho.setConfirmadoEm(LocalDateTime.now());
        rascunho.setDocumentoResultanteId(documento.id());
        rascunhoRepository.save(rascunho);

        return "Confirmado. Documento " + documento.id() + " agora está na versão " + documento.versaoAtual() + ".";
    }

    @Tool("""
            Descarta a última proposta de criação/atualização pendente nesta conversa, caso \
            o usuário decida explicitamente não seguir com ela (inclusive uma que ainda \
            esteja sendo gerada, ou que tenha falhado ao gerar). Nada é salvo como documento.""")
    public String descartarRascunhoPendente() {

        var contexto = ConversaContextHolder.atual();

        Optional<RascunhoDocumentoEntity> rascunhoOpt = rascunhoRepository
                .findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunho.ativos());

        if (rascunhoOpt.isEmpty()) {
            return "Não há nenhuma proposta pendente para descartar nesta conversa.";
        }

        // R1: um rascunho em GERANDO também pode ser descartado. UPDATE condicional só na
        // coluna status (nunca um save() da entidade inteira, lida antes desta chamada) —
        // se o worker terminar (finalizarComSucesso/finalizarComErro) ENTRE o findFirst
        // acima e este descartar(), o resultado dele já está commitado com status != GERANDO
        // e este UPDATE simplesmente não encontra mais a linha elegível (0 linhas afetadas,
        // tratado abaixo); nunca reescreve/apaga titulo/conteudoHtml/conteudoEstruturado já
        // gravados pelo worker, que é o bug que esta troca corrige (ver
        // IRascunhoDocumentoRepositoryGeracaoAssincronaTest).
        int descartados = rascunhoRepository.descartar(rascunhoOpt.get().getId(), StatusRascunho.ativos());
        if (descartados == 0) {
            return "Essa proposta não está mais ativa — talvez já tenha terminado de gerar ou já tenha "
                    + "sido descartada. Confira o painel antes de tentar de novo.";
        }

        return "Proposta descartada. Nada foi salvo.";
    }
}
