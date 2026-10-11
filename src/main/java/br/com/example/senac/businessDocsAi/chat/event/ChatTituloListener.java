package br.com.example.senac.businessDocsAi.chat.event;

import br.com.example.senac.businessDocsAi.chat.entity.ConversaEntity;
import br.com.example.senac.businessDocsAi.chat.repository.IConversaRepository;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gera automaticamente o título de uma conversa a partir da primeira mensagem do usuário com
 * conteúdo substancial — em background (depois do commit, em outra thread), pra nunca atrasar
 * a resposta do chat. Mensagens curtas (ex.: "olá") são ignoradas de propósito: um título
 * gerado só a partir de uma saudação ficaria vago — e, como isso só roda enquanto a conversa
 * ainda não tem título, um título vago travaria aí para sempre. É melhor esperar a primeira
 * mensagem com assunto real, mesmo que isso signifique a conversa ficar sem título por mais um
 * turno ou dois.
 */
@Component
@RequiredArgsConstructor
public class ChatTituloListener {

    private static final Logger log = LoggerFactory.getLogger(ChatTituloListener.class);
    private static final int TAMANHO_MINIMO_PARA_TITULAR = 20;
    private static final int TAMANHO_MAXIMO_TITULO = 80;

    private final IConversaRepository conversaRepository;
    private final ChatModel chatModel;

    @Async("tituloExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoReceberMensagem(ConversaMensagemRecebidaEvent event) {

        String pergunta = event.perguntaTexto();
        if (pergunta == null || pergunta.trim().length() < TAMANHO_MINIMO_PARA_TITULAR) {
            return;
        }

        ConversaEntity conversa = conversaRepository.findById(event.conversaId()).orElse(null);
        if (conversa == null || conversa.getTitulo() != null) {
            return;
        }

        String titulo = gerarTitulo(pergunta);
        if (titulo == null) {
            return;
        }

        conversa.setTitulo(titulo);
        conversaRepository.save(conversa);
    }

    private String gerarTitulo(String mensagem) {
        try {
            String prompt = """
                    Resuma o assunto da mensagem abaixo em um título curto para o histórico de \
                    uma conversa de chat — no máximo 6 palavras, sem aspas, sem ponto final, em \
                    português. Responda SOMENTE com o título, nada mais.

                    Mensagem:
                    %s""".formatted(mensagem);

            String resposta = chatModel.chat(prompt);
            if (resposta == null) {
                return null;
            }

            String titulo = resposta.trim().replaceAll("^[\"'“]+|[\"'”]+$", "");
            if (titulo.isBlank()) {
                return null;
            }

            return titulo.length() > TAMANHO_MAXIMO_TITULO ? titulo.substring(0, TAMANHO_MAXIMO_TITULO) : titulo;
        } catch (Exception e) {
            log.warn("Falha ao gerar título automático de conversa", e);
            return null;
        }
    }
}
