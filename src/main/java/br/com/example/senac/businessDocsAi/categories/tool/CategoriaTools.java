package br.com.example.senac.businessDocsAi.categories.tool;

import br.com.example.senac.businessDocsAi.categories.dto.CategoryRequestDTO;
import br.com.example.senac.businessDocsAi.categories.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.entity.CategoriaRascunhoEntity;
import br.com.example.senac.businessDocsAi.categories.entity.StatusRascunhoCategoria;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoriaRascunhoRepository;
import br.com.example.senac.businessDocsAi.categories.service.CategoryService;
import br.com.example.senac.businessDocsAi.chat.service.ConversaContextHolder;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Ferramentas de IA para propor/criar categorias via chat quando nenhuma categoria
 * existente serve para o documento em questão — alternativa a precisar ir até a tela de
 * categorias (que continua existindo normalmente, ver {@code CategoryController}).
 *
 * <p>Segue o mesmo princípio de {@code DocumentoTools}: nunca cria direto, só prepara um
 * rascunho; confirmar só funciona num turno posterior ao da proposta. Criar categoria de
 * verdade continua sendo uma ação só de ADMIN (igual à tela) — {@code CategoryService.save}
 * já aplica essa regra, então EDITOR pode propor pelo chat, mas a confirmação é recusada com
 * uma mensagem clara se quem confirmar não for admin.</p>
 */
@Component
@RequiredArgsConstructor
public class CategoriaTools {

    private static final Logger log = LoggerFactory.getLogger(CategoriaTools.class);

    private final CategoryService categoryService;
    private final ICategoriaRascunhoRepository categoriaRascunhoRepository;

    @Tool("""
            Prepara uma PROPOSTA de criação de uma categoria nova — use somente quando \
            listarMinhasCategorias não trouxe nenhuma categoria adequada para o documento \
            que está sendo criado/atualizado. NÃO cria nada ainda, só grava um rascunho. \
            Depois de chamar esta ferramenta, mostre o nome e a descrição propostos ao \
            usuário e pergunte se ele confirma. NÃO chame confirmarCriacaoCategoria na mesma \
            resposta — só depois que o usuário confirmar numa mensagem separada.""")
    public String prepararCriacaoCategoria(
            @P("nome da categoria proposta") String nome,
            @P("descrição curta da categoria proposta") String descricao
    ) {
        var contexto = ConversaContextHolder.atual();

        // Mesma lógica de DocumentoTools: se já existe um rascunho pendente desta conversa,
        // atualiza-o preservando o turnoCriacao original, em vez de resetar o relógio a cada
        // "reproposta" (o modelo às vezes refaz a proposta antes de confirmar).
        CategoriaRascunhoEntity rascunho = categoriaRascunhoRepository
                .findFirstByConversaIdAndStatusOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunhoCategoria.PENDENTE)
                .orElseGet(CategoriaRascunhoEntity::new);

        boolean novo = rascunho.getId() == null;

        rascunho.setConversaId(contexto.conversaId());
        rascunho.setNome(nome);
        rascunho.setDescricao(descricao);
        rascunho.setStatus(StatusRascunhoCategoria.PENDENTE);
        if (novo) {
            rascunho.setTurnoCriacao(contexto.turnoAtual());
            rascunho.setCriadoEm(LocalDateTime.now());
        }

        categoriaRascunhoRepository.save(rascunho);

        return """
                Rascunho de categoria preparado (ainda não salvo). Mostre isto ao usuário e \
                peça confirmação explícita antes de chamar confirmarCriacaoCategoria:

                Nome: %s
                Descrição: %s""".formatted(nome, descricao);
    }

    @Tool("""
            Confirma e efetiva a última proposta de criação de categoria feita nesta \
            conversa (a que foi preparada com prepararCriacaoCategoria). SÓ chame isto \
            depois que o usuário confirmar explicitamente, em uma mensagem à parte, que \
            quer seguir com a proposta — nunca no mesmo turno em que ela foi feita. Criar \
            categoria é uma ação restrita a administradores; se quem está confirmando não \
            for admin, isto vai recusar e explicar.""")
    public String confirmarCriacaoCategoria() {

        var contexto = ConversaContextHolder.atual();

        Optional<CategoriaRascunhoEntity> rascunhoOpt = categoriaRascunhoRepository
                .findFirstByConversaIdAndStatusOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunhoCategoria.PENDENTE);

        if (rascunhoOpt.isEmpty()) {
            return "Não há nenhuma proposta de categoria pendente para confirmar nesta conversa.";
        }

        CategoriaRascunhoEntity rascunho = rascunhoOpt.get();

        if (rascunho.getTurnoCriacao().equals(contexto.turnoAtual())) {
            log.warn(
                    "Tentativa de confirmar categoria {} no mesmo turno em que foi proposta (conversa {})",
                    rascunho.getId(), contexto.conversaId()
            );
            return "Ainda não é possível confirmar: a proposta precisa ser mostrada ao usuário "
                    + "e confirmada em uma mensagem separada antes de ser efetivada.";
        }

        CategoryResponseDTO categoria;
        try {
            categoria = categoryService.save(new CategoryRequestDTO(rascunho.getNome(), rascunho.getDescricao()));
        } catch (AccessDeniedException e) {
            return "Somente administradores podem criar categorias novas. Peça para um "
                    + "administrador criar a categoria \"" + rascunho.getNome() + "\" (ou vincular "
                    + "você a uma categoria já existente) e tente de novo.";
        }

        rascunho.setStatus(StatusRascunhoCategoria.CONFIRMADO);
        rascunho.setConfirmadoEm(LocalDateTime.now());
        categoriaRascunhoRepository.save(rascunho);

        return "Confirmado. Categoria \"" + categoria.name() + "\" criada (ID " + categoria.id() + ").";
    }

    @Tool("""
            Descarta a última proposta de criação de categoria pendente nesta conversa, \
            caso o usuário decida explicitamente não seguir com ela.""")
    public String descartarCriacaoCategoria() {

        var contexto = ConversaContextHolder.atual();

        Optional<CategoriaRascunhoEntity> rascunhoOpt = categoriaRascunhoRepository
                .findFirstByConversaIdAndStatusOrderByCriadoEmDesc(contexto.conversaId(), StatusRascunhoCategoria.PENDENTE);

        if (rascunhoOpt.isEmpty()) {
            return "Não há nenhuma proposta de categoria pendente para descartar nesta conversa.";
        }

        CategoriaRascunhoEntity rascunho = rascunhoOpt.get();
        rascunho.setStatus(StatusRascunhoCategoria.DESCARTADO);
        categoriaRascunhoRepository.save(rascunho);

        return "Proposta de categoria descartada. Nada foi salvo.";
    }
}
