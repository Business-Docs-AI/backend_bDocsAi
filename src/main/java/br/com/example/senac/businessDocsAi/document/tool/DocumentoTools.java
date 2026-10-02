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

    @Tool("""
            Prepara uma PROPOSTA de criação de um documento novo — NÃO cria nada ainda, só \
            grava um rascunho. Chame listarMinhasCategorias antes para saber o ID da \
            categoria correta. Depois de chamar esta ferramenta, reproduza o título e o \
            CONTEÚDO COMPLETO retornados por ela, na íntegra, na sua resposta ao usuário — \
            NÃO resuma, NÃO liste apenas os tópicos/seções do documento. O usuário precisa \
            ler o texto literal e completo para revisar e decidir se aprova, exatamente \
            como ficaria salvo. Depois disso, pergunte se ele confirma. NÃO chame \
            confirmarRascunhoPendente na mesma resposta — só depois que o usuário \
            confirmar numa mensagem separada.""")
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
        // isso: havendo rascunho pendente desta conversa, atualiza o conteúdo dele mas
        // preserva o turnoCriacao original.
        RascunhoDocumentoEntity rascunho = rascunhoRepository
                .findFirstByConversaIdAndStatusOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunho.PENDENTE)
                .orElseGet(RascunhoDocumentoEntity::new);

        boolean novo = rascunho.getId() == null;

        rascunho.setConversaId(contexto.conversaId());
        rascunho.setTipo(TipoRascunho.CRIAR);
        rascunho.setDocumentoIdAlvo(null);
        rascunho.setCategoriaId(categoriaId);
        rascunho.setTitulo(titulo);
        rascunho.setConteudoHtml(conteudoHtml);
        rascunho.setStatus(StatusRascunho.PENDENTE);
        if (novo) {
            rascunho.setTurnoCriacao(contexto.turnoAtual());
            rascunho.setCriadoEm(LocalDateTime.now());
        }

        rascunhoRepository.save(rascunho);

        return """
                Rascunho de CRIAÇÃO preparado (ainda não salvo). IMPORTANTE: copie o título \
                e o conteúdo abaixo INTEGRALMENTE na sua próxima mensagem ao usuário — NÃO \
                resuma, NÃO parafraseie, NÃO liste só as seções/tópicos. O usuário precisa \
                ler o texto real e completo, exatamente como ficaria salvo, para poder \
                revisar e aprovar com segurança. Só depois disso peça a confirmação:

                Título: %s

                Conteúdo (HTML):
                %s""".formatted(titulo, conteudoHtml);
    }

    @Tool("""
            Prepara uma PROPOSTA de atualização de um documento já existente — NÃO atualiza \
            nada ainda, só grava um rascunho. Use buscarDocumentos antes para achar o ID \
            certo. Depois de chamar esta ferramenta, reproduza o novo título e o novo \
            CONTEÚDO COMPLETO retornados por ela, na íntegra, na sua resposta ao usuário — \
            NÃO resuma, NÃO liste apenas os tópicos/seções. O usuário precisa ler o texto \
            literal e completo para revisar e decidir se aprova, exatamente como ficaria \
            salvo. Depois disso, pergunte se ele confirma. NÃO chame \
            confirmarRascunhoPendente na mesma resposta — só depois que o usuário \
            confirmar numa mensagem separada.""")
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

        // Ver prepararCriacaoDocumento: preserva o turnoCriacao de um rascunho pendente já
        // existente desta conversa em vez de resetá-lo a cada "reproposta".
        RascunhoDocumentoEntity rascunho = rascunhoRepository
                .findFirstByConversaIdAndStatusOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunho.PENDENTE)
                .orElseGet(RascunhoDocumentoEntity::new);

        boolean novo = rascunho.getId() == null;

        rascunho.setConversaId(contexto.conversaId());
        rascunho.setTipo(TipoRascunho.ATUALIZAR);
        rascunho.setDocumentoIdAlvo(id);
        rascunho.setCategoriaId(documentoAtual.categoriaId());
        rascunho.setTitulo(titulo);
        rascunho.setConteudoHtml(conteudoHtml);
        rascunho.setStatus(StatusRascunho.PENDENTE);
        if (novo) {
            rascunho.setTurnoCriacao(contexto.turnoAtual());
            rascunho.setCriadoEm(LocalDateTime.now());
        }

        rascunhoRepository.save(rascunho);

        return """
                Rascunho de ATUALIZAÇÃO do documento %s preparado (ainda não salvo). \
                IMPORTANTE: copie o título e o conteúdo abaixo INTEGRALMENTE na sua \
                próxima mensagem ao usuário — NÃO resuma, NÃO parafraseie, NÃO liste só as \
                seções/tópicos. O usuário precisa ler o texto real e completo, exatamente \
                como ficaria salvo, para poder revisar e aprovar com segurança. Só depois \
                disso peça a confirmação:

                Novo título: %s

                Novo conteúdo (HTML):
                %s""".formatted(id, titulo, conteudoHtml);
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
                .findFirstByConversaIdAndStatusOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunho.PENDENTE);

        if (rascunhoOpt.isEmpty()) {
            return "Não há nenhuma proposta pendente para confirmar nesta conversa.";
        }

        RascunhoDocumentoEntity rascunho = rascunhoOpt.get();

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

        DocumentoRequestDTO dto = new DocumentoRequestDTO(
                rascunho.getTitulo(), rascunho.getConteudoHtml(), "Criado/atualizado via chat com IA",
                rascunho.getCategoriaId()
        );

        DocumentoResponseDTO documento = rascunho.getTipo() == TipoRascunho.CRIAR
                ? documentoService.criar(dto)
                : documentoService.atualizar(rascunho.getDocumentoIdAlvo(), dto);

        rascunho.setStatus(StatusRascunho.CONFIRMADO);
        rascunho.setConfirmadoEm(LocalDateTime.now());
        rascunhoRepository.save(rascunho);

        return "Confirmado. Documento " + documento.id() + " agora está na versão " + documento.versaoAtual() + ".";
    }

    @Tool("""
            Descarta a última proposta de criação/atualização pendente nesta conversa, caso \
            o usuário decida explicitamente não seguir com ela. Nada é salvo como documento.""")
    public String descartarRascunhoPendente() {

        var contexto = ConversaContextHolder.atual();

        Optional<RascunhoDocumentoEntity> rascunhoOpt = rascunhoRepository
                .findFirstByConversaIdAndStatusOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunho.PENDENTE);

        if (rascunhoOpt.isEmpty()) {
            return "Não há nenhuma proposta pendente para descartar nesta conversa.";
        }

        RascunhoDocumentoEntity rascunho = rascunhoOpt.get();
        rascunho.setStatus(StatusRascunho.DESCARTADO);
        rascunhoRepository.save(rascunho);

        return "Proposta descartada. Nada foi salvo.";
    }
}
