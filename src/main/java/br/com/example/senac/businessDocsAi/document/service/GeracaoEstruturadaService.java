package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.chat.entity.MensagemEntity;
import br.com.example.senac.businessDocsAi.chat.entity.Papel;
import br.com.example.senac.businessDocsAi.chat.repository.IMensagemRepository;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTO;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Worker de geração assíncrona do documento estruturado (Etapa 13.4). Chamado pelo {@code
 * GeracaoEstruturadaListener} (reação imediata) e pelo {@code GeracaoEstruturadaJob} (rede
 * de segurança) — nunca pela requisição HTTP original (R2: roda fora da requisição, nada de
 * {@code SecurityContext}/{@code ConversaContextHolder}; tudo que precisa já está em colunas
 * de {@link RascunhoDocumentoEntity}).
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bdocs.documentacao-estruturada", name = "enabled", havingValue = "true")
public class GeracaoEstruturadaService {

    private static final Logger log = LoggerFactory.getLogger(GeracaoEstruturadaService.class);

    // Laço de correção (decisão A2) DENTRO de uma mesma tentativa — sem usuário no meio, o
    // worker devolve os erros de validação pro modelo corrigir na mesma "conversa" (mesma
    // ChatMemory). Diferente de tentativasGeracao (R3: reservas entre o listener e o job de
    // segurança, cada uma conta como 1 tentativa de verdade, com custo de API completo).
    private static final int MAX_CORRECOES_POR_TENTATIVA = 2;
    private static final int JANELA_MEMORIA_MENSAGENS = 20;

    private final IRascunhoDocumentoRepository rascunhoRepository;
    private final IMensagemRepository mensagemRepository;
    private final DocumentoEstruturadoValidator validator;
    private final Validator beanValidator;
    private final EstruturaDocumentoHtmlRenderer renderer;
    private final ObjectMapper objectMapper;

    @Qualifier("chatModelGeracaoEstruturada")
    private final ChatModel chatModelGeracaoEstruturada;

    @Value("${bdocs.documentacao-estruturada.geracao.max-tentativas}")
    private int maxTentativas;

    @Value("${bdocs.documentacao-estruturada.geracao.timeout-gerando-minutos}")
    private int timeoutGerandoMinutos;

    @Value("${bdocs.documentacao-estruturada.geracao.material-max-caracteres}")
    private int materialMaxCaracteres;

    interface Assistente {
        String gerar(@UserMessage String material);

        String corrigir(@UserMessage String mensagemDeCorrecao);
    }

    static class FerramentaCaptura {
        DocumentoEstruturadoDTO capturado;
        int chamadas = 0;

        @Tool("Registra o documento estruturado extraido do material fornecido pelo usuario.")
        public String registrarDocumentoEstruturado(DocumentoEstruturadoDTO documento) {
            capturado = documento;
            chamadas++;
            return GeracaoEstruturadaSystemPrompt.RETORNO_FERRAMENTA_PRIMEIRA_CHAMADA;
        }
    }

    /**
     * R3: reserva atomicamente o rascunho (nunca processa o mesmo rascunho 2x) e, se
     * conseguir, tenta gerar o documento. affected=0 na reserva significa "outro worker já
     * pegou" ou "não está mais elegível" (ex.: usuário descartou) — desiste em silêncio.
     */
    public void processar(UUID rascunhoId) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime expiradoAntesDe = agora.minusMinutes(timeoutGerandoMinutos);

        int reservado = rascunhoRepository.reservarParaProcessamento(rascunhoId, agora, expiradoAntesDe, maxTentativas);
        if (reservado == 0) {
            return;
        }

        RascunhoDocumentoEntity rascunho = rascunhoRepository.findById(rascunhoId).orElse(null);
        if (rascunho == null) {
            return;
        }

        String material = montarMaterial(rascunho);
        if (material.length() > materialMaxCaracteres) {
            rascunhoRepository.finalizarComErro(rascunhoId, (
                    "O material desta conversa (%d caracteres) excede o limite de %d "
                            + "caracteres para geração estruturada. Reduza o conteúdo da "
                            + "conversa (ex.: numa conversa nova) e peça para gerar de novo."
            ).formatted(material.length(), materialMaxCaracteres));
            return;
        }

        org.slf4j.MDC.put("rascunhoId", rascunhoId.toString());
        try {
            DocumentoEstruturadoDTO documento = gerarDocumentoValidado(material);

            String html = renderer.renderizar(documento);
            String conteudoCompletoJson = serializar(documento);

            int afetados = rascunhoRepository.finalizarComSucesso(
                    rascunhoId, documento.titulo(), html, conteudoCompletoJson,
                    DocumentoEstruturadoDTO.VERSAO_SCHEMA_ATUAL, rascunho.getCategoriaId()
            );
            if (afetados == 0) {
                log.info(
                        "Rascunho {} não estava mais GERANDO ao terminar a geração (provavelmente "
                                + "descartado pelo usuário) — resultado descartado (R1).",
                        rascunhoId
                );
            }
        } catch (DocumentoEstruturadoInvalidoException e) {
            if (rascunho.getTentativasGeracao() >= maxTentativas) {
                rascunhoRepository.finalizarComErro(rascunhoId, (
                        "Não foi possível gerar um documento estruturado válido. Últimos "
                                + "problemas encontrados: " + String.join("; ", e.getErros())
                ));
            }
            // Senão, fica em GERANDO: o job de segurança tenta de novo quando a reserva expirar.
        } catch (Exception e) {
            log.warn(
                    "Falha ao gerar documento estruturado para o rascunho {} (tentativa {}/{}): {}",
                    rascunhoId, rascunho.getTentativasGeracao(), maxTentativas, e.getClass().getSimpleName()
            );
            if (rascunho.getTentativasGeracao() >= maxTentativas) {
                rascunhoRepository.finalizarComErro(rascunhoId, (
                        "Falha ao gerar o documento (%s). Tentativas esgotadas."
                ).formatted(e.getClass().getSimpleName()));
            }
            // Senão, fica em GERANDO: o job de segurança tenta de novo quando a reserva expirar.
        } finally {
            org.slf4j.MDC.remove("rascunhoId");
        }
    }

    /**
     * Chama a IA (com o laço de correção interno — decisão A2) e devolve um documento JÁ
     * validado (Bean + semântico), ou lança {@link DocumentoEstruturadoInvalidoException}
     * se não conseguir um documento válido dentro do laço. Extraído como método próprio
     * (não {@code private}) para ser substituído por um dublê em teste, sem precisar de um
     * {@code ChatModel} fake reproduzindo o protocolo inteiro de tool-calling do langchain4j
     * só para testar a ORQUESTRAÇÃO (reserva, limite de material, decisão de retentar vs.
     * desistir) — essa parte é testada separadamente, com um {@code ChatModel} fake de
     * verdade (ver {@code GeracaoEstruturadaServiceTest}).
     */
    protected DocumentoEstruturadoDTO gerarDocumentoValidado(String material) {
        FerramentaCaptura ferramenta = new FerramentaCaptura();
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(JANELA_MEMORIA_MENSAGENS);

        Assistente assistente = AiServices.builder(Assistente.class)
                .chatModel(chatModelGeracaoEstruturada)
                .chatMemory(chatMemory)
                .systemMessageProvider(id -> GeracaoEstruturadaSystemPrompt.TEXTO)
                .tools(ferramenta)
                .build();

        assistente.gerar(material);
        List<String> erros = validarCapturado(ferramenta);

        int correcoes = 0;
        while (!erros.isEmpty() && correcoes < MAX_CORRECOES_POR_TENTATIVA) {
            correcoes++;
            log.info(
                    "Documento estruturado inválido na correção {}/{} ({} erro(s))",
                    correcoes, MAX_CORRECOES_POR_TENTATIVA, erros.size()
            );
            ferramenta.capturado = null;
            assistente.corrigir(
                    "O documento tem os seguintes problemas - corrija e chame a ferramenta "
                            + "de registro de novo com o documento corrigido:\n- "
                            + String.join("\n- ", erros)
            );
            erros = validarCapturado(ferramenta);
        }

        if (!erros.isEmpty()) {
            throw new DocumentoEstruturadoInvalidoException(erros);
        }

        return ferramenta.capturado;
    }

    // Bean Validation + validação semântica (DocumentoEstruturadoValidator) — "validação
    // Bean + semântica" do desenho aprovado.
    List<String> validarCapturado(FerramentaCaptura ferramenta) {
        if (ferramenta.capturado == null) {
            return List.of("O modelo não chamou a ferramenta de registro do documento.");
        }

        List<String> erros = new ArrayList<>();
        for (ConstraintViolation<DocumentoEstruturadoDTO> violacao : beanValidator.validate(ferramenta.capturado)) {
            erros.add(violacao.getPropertyPath() + ": " + violacao.getMessage());
        }
        erros.addAll(validator.validar(ferramenta.capturado));
        return erros;
    }

    private String serializar(DocumentoEstruturadoDTO documento) {
        try {
            return objectMapper.writeValueAsString(documento);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar o documento estruturado gerado", e);
        }
    }

    // R4: material-fonte = histórico PERSISTIDO da conversa até o momento da solicitação
    // (nunca mensagens de turnos posteriores) + instruções adicionais da tool. Anexos/áudio
    // já vêm com o texto extraído DENTRO de MensagemEntity.conteudo (ver ChatService) — nada
    // de reextração aqui.
    String montarMaterial(RascunhoDocumentoEntity rascunho) {
        List<MensagemEntity> mensagens = mensagemRepository
                .findByConversaIdAndCriadoEmLessThanEqualOrderByCriadoEmAsc(
                        rascunho.getConversaId(), rascunho.getCriadoEm()
                );

        StringBuilder material = new StringBuilder();
        for (MensagemEntity mensagem : mensagens) {
            material.append(mensagem.getPapel() == Papel.USER ? "Usuário: " : "Assistente: ")
                    .append(mensagem.getConteudo())
                    .append("\n\n");
        }

        if (rascunho.getInstrucoesAdicionais() != null && !rascunho.getInstrucoesAdicionais().isBlank()) {
            material.append("Instruções adicionais para focar a geração: ")
                    .append(rascunho.getInstrucoesAdicionais());
        }

        return material.toString();
    }
}
