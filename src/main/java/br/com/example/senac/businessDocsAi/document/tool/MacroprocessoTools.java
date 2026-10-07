package br.com.example.senac.businessDocsAi.document.tool;

import br.com.example.senac.businessDocsAi.document.dto.MacroprocessoResponseDTO;
import br.com.example.senac.businessDocsAi.document.service.MacroprocessoService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ferramenta de IA só para LEITURA de macroprocessos (padrão de {@code listarMinhasCategorias})
 * — usada pelo caminho de documento estruturado (Etapa 13) para escolher um macroprocesso
 * existente. Criar macroprocesso novo não é feito pelo chat (decisão C2c: caminho menos
 * intrusivo é o CRUD ADMIN de {@code MacroprocessoController|Service}); se nenhum servir, a
 * IA deve registrar isso como pendência, nunca inventar um.
 *
 * <p>Só existe com a feature flag ligada — com ela desligada, este bean nem é criado, e a
 * ferramenta não aparece pro LLM (ver {@code RagAssistantConfig}, injeção opcional).</p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled", havingValue = "true")
public class MacroprocessoTools {

    private final MacroprocessoService macroprocessoService;

    @Tool("""
            Lista os macroprocessos existentes, para saber em qual encaixar um documento do \
            tipo PROCESSO ou PROCEDIMENTO. Se nenhum servir para o assunto do documento, NÃO \
            crie um novo — registre isso como pendência e siga sem macroprocesso, ou pergunte \
            ao usuário se prefere pedir a um administrador para criar um.""")
    public String listarMacroprocessos() {
        var macroprocessos = macroprocessoService.listar();

        if (macroprocessos.isEmpty()) {
            return "Não há nenhum macroprocesso cadastrado ainda.";
        }

        StringBuilder texto = new StringBuilder();
        for (MacroprocessoResponseDTO macroprocesso : macroprocessos) {
            texto.append("ID: ").append(macroprocesso.id())
                    .append(" | Nome: ").append(macroprocesso.nome())
                    .append("\n");
        }
        return texto.toString();
    }
}
