package br.com.example.senac.businessDocsAi.ai.generation;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

import java.util.UUID;

/**
 * Assistente de IA Service do LangChain4j. Não recebe nenhuma {@code .tools(...)} na
 * configuração (ver {@link RagAssistantConfig}) — não existe, portanto, nenhuma ferramenta
 * disponível para alterar documentos; ele só lê o contexto recuperado e responde.
 */
public interface RagAssistant {

    Result<String> responder(@MemoryId UUID conversaId, @UserMessage String pergunta);
}
