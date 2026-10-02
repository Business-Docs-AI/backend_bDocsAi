package br.com.example.senac.businessDocsAi.chat.service;

import br.com.example.senac.businessDocsAi.ai.generation.RagAssistant;
import br.com.example.senac.businessDocsAi.categories.repository.ICategoriaRascunhoRepository;
import br.com.example.senac.businessDocsAi.chat.dto.ConversaResponseDTO;
import br.com.example.senac.businessDocsAi.chat.dto.CriarConversaRequestDTO;
import br.com.example.senac.businessDocsAi.chat.dto.FonteDTO;
import br.com.example.senac.businessDocsAi.chat.dto.MensagemRequestDTO;
import br.com.example.senac.businessDocsAi.chat.dto.MensagemResponseDTO;
import br.com.example.senac.businessDocsAi.chat.entity.ConversaEntity;
import br.com.example.senac.businessDocsAi.chat.entity.MensagemEntity;
import br.com.example.senac.businessDocsAi.chat.entity.Papel;
import br.com.example.senac.businessDocsAi.chat.repository.IConversaRepository;
import br.com.example.senac.businessDocsAi.chat.repository.IMensagemRepository;
import br.com.example.senac.businessDocsAi.document.repository.IRascunhoDocumentoRepository;
import br.com.example.senac.businessDocsAi.exception.BadRequestException;
import br.com.example.senac.businessDocsAi.exception.NotFoundException;
import br.com.example.senac.businessDocsAi.security.CurrentUserProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Conversas isoladas por usuário: toda leitura/escrita passa por
 * {@link #buscarConversaDoUsuarioOrElseThrow(UUID)}, que usa
 * {@code findByIdAndUsuarioId} — uma conversa de outro usuário simplesmente "não existe"
 * para quem consulta, virando 404 em vez de vazar a existência dela com um 403.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final IConversaRepository conversaRepository;
    private final IMensagemRepository mensagemRepository;
    private final IRascunhoDocumentoRepository rascunhoRepository;
    private final ICategoriaRascunhoRepository categoriaRascunhoRepository;

    @Qualifier("ragAssistantSomenteLeitura")
    private final RagAssistant ragAssistantSomenteLeitura;

    @Qualifier("ragAssistantComFerramentas")
    private final RagAssistant ragAssistantComFerramentas;

    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;
    private final MarkdownConversorService markdownConversorService;
    private final AudioTranscricaoService audioTranscricaoService;
    private final AnexoTextoExtractorService anexoTextoExtractorService;
    private final ArmazenamentoAnexoService armazenamentoAnexoService;
    private final PersistentChatMemoryStore persistentChatMemoryStore;

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ConversaResponseDTO criarConversa(CriarConversaRequestDTO dto) {

        ConversaEntity conversa = new ConversaEntity();
        conversa.setUsuarioId(currentUserProvider.getCurrentUserId());
        conversa.setTitulo(dto == null ? null : dto.titulo());
        conversa.setCriadoEm(LocalDateTime.now());

        ConversaEntity salva = conversaRepository.save(conversa);

        return toConversaResponseDTO(salva);
    }

    @PreAuthorize("isAuthenticated()")
    public List<ConversaResponseDTO> listarConversas() {

        Long usuarioId = currentUserProvider.getCurrentUserId();

        return conversaRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
                .map(this::toConversaResponseDTO)
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    public List<MensagemResponseDTO> buscarConversa(UUID conversaId) {

        buscarConversaDoUsuarioOrElseThrow(conversaId);

        return mensagemRepository.findByConversaIdOrderByCriadoEmAsc(conversaId).stream()
                .map(this::toMensagemResponseDTO)
                .toList();
    }

    // Apaga tudo que pertence à conversa: mensagens, rascunhos de documento e de categoria
    // propostos nela (confirmados ou não) e qualquer áudio/anexo enviado — nada da conversa
    // sobrevive à sua exclusão. O documento/categoria em si, se a proposta já tiver sido
    // confirmada antes da exclusão, não é afetado: é uma entidade independente a partir daí.
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void excluirConversa(UUID conversaId) {

        ConversaEntity conversa = buscarConversaDoUsuarioOrElseThrow(conversaId);

        rascunhoRepository.deleteByConversaId(conversaId);
        categoriaRascunhoRepository.deleteByConversaId(conversaId);
        mensagemRepository.deleteByConversaId(conversaId);
        conversaRepository.delete(conversa);

        armazenamentoAnexoService.excluirTudoDaConversa(conversaId);
        persistentChatMemoryStore.invalidar(conversaId);
    }

    // Histórico completo da conversa em Markdown, para o usuário salvar localmente antes de
    // decidir excluir a conversa (ou só para registro) quando uma proposta não foi aprovada.
    @PreAuthorize("isAuthenticated()")
    public String exportarConversaMarkdown(UUID conversaId) {

        ConversaEntity conversa = buscarConversaDoUsuarioOrElseThrow(conversaId);

        List<MensagemEntity> mensagens = mensagemRepository.findByConversaIdOrderByCriadoEmAsc(conversaId);

        StringBuilder md = new StringBuilder();
        md.append("# ").append(conversa.getTitulo() != null ? conversa.getTitulo() : "Conversa sem título")
                .append("\n\n");
        md.append("_Criada em: ").append(conversa.getCriadoEm()).append("_\n\n");
        md.append("---\n\n");

        for (MensagemEntity mensagem : mensagens) {
            String autor = mensagem.getPapel() == Papel.USER ? "**Usuário**" : "**Assistente**";
            md.append(autor).append(" (").append(mensagem.getCriadoEm()).append("):\n\n");
            md.append(mensagem.getConteudo()).append("\n\n");

            List<FonteDTO> fontes = desserializarFontes(mensagem.getFontes());
            if (!fontes.isEmpty()) {
                md.append("Fontes:\n");
                for (FonteDTO fonte : fontes) {
                    md.append("- [").append(fonte.titulo()).append("](").append(fonte.link()).append(")\n");
                }
                md.append("\n");
            }

            md.append("---\n\n");
        }

        return md.toString();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public MensagemResponseDTO enviarMensagem(UUID conversaId, MensagemRequestDTO dto) {

        buscarConversaDoUsuarioOrElseThrow(conversaId);

        String perguntaTexto = markdownConversorService.converterSeNecessario(dto.pergunta());

        return processarPerguntaEResponder(conversaId, perguntaTexto);
    }

    // Mesma rota que enviarMensagem, mas aceita texto, áudio e/ou um documento anexado
    // (multipart) — pelo menos um dos três precisa vir preenchido. Áudio é transcrito e o
    // anexo tem seu texto extraído (ver AudioTranscricaoService/AnexoTextoExtractorService);
    // o resultado de cada um entra como uma seção rotulada no texto final, que passa pelo
    // mesmo pipeline de uma mensagem de texto comum (inclusive a conversão para Markdown de
    // textos grandes). O arquivo original é guardado em disco (ArmazenamentoAnexoService),
    // associado à conversa — não a uma mensagem específica — e é removido junto quando a
    // conversa é excluída.
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public MensagemResponseDTO enviarMensagemComArquivo(
            UUID conversaId, String pergunta, MultipartFile audio, MultipartFile anexo
    ) {

        buscarConversaDoUsuarioOrElseThrow(conversaId);

        boolean temTexto = pergunta != null && !pergunta.isBlank();
        boolean temAudio = audio != null && !audio.isEmpty();
        boolean temAnexo = anexo != null && !anexo.isEmpty();

        if (!temTexto && !temAudio && !temAnexo) {
            throw new BadRequestException("Envie um texto, um áudio ou um documento anexado.");
        }

        StringBuilder perguntaTexto = new StringBuilder();

        if (temTexto) {
            perguntaTexto.append(markdownConversorService.converterSeNecessario(pergunta));
        }

        if (temAudio) {
            String transcricao = audioTranscricaoService.transcrever(audio);
            adicionarSecao(perguntaTexto, "Transcrição do áudio enviado", transcricao);
            armazenamentoAnexoService.salvar(conversaId, audio);
        }

        if (temAnexo) {
            String textoExtraido = anexoTextoExtractorService.extrairTexto(anexo);
            String textoConvertido = markdownConversorService.converterSeNecessario(textoExtraido);
            adicionarSecao(
                    perguntaTexto, "Conteúdo do arquivo anexado: " + anexo.getOriginalFilename(), textoConvertido
            );
            armazenamentoAnexoService.salvar(conversaId, anexo);
        }

        return processarPerguntaEResponder(conversaId, perguntaTexto.toString());
    }

    private void adicionarSecao(StringBuilder texto, String titulo, String conteudo) {
        if (!texto.isEmpty()) {
            texto.append("\n\n");
        }
        texto.append("[").append(titulo).append("]\n").append(conteudo);
    }

    // Persiste a pergunta (já resolvida para texto, seja ela digitada, transcrita ou
    // extraída de um anexo), chama o assistente adequado ao papel do usuário e persiste a
    // resposta — fluxo comum a enviarMensagem e enviarMensagemComArquivo.
    private MensagemResponseDTO processarPerguntaEResponder(UUID conversaId, String perguntaTexto) {

        MensagemEntity pergunta = new MensagemEntity();
        pergunta.setConversaId(conversaId);
        pergunta.setPapel(Papel.USER);
        pergunta.setConteudo(perguntaTexto);
        pergunta.setCriadoEm(LocalDateTime.now());
        mensagemRepository.save(pergunta);

        // Cada chamada é um "turno" novo — é o que impede DocumentoTools de confirmar um
        // rascunho proposto na mesma resposta em que foi criado (ver ConversaContextHolder).
        UUID turnoAtual = UUID.randomUUID();
        ConversaContextHolder.iniciar(conversaId, turnoAtual);

        Result<String> resultado;
        try {
            RagAssistant assistente = currentUserProvider.isEditorOuAdmin()
                    ? ragAssistantComFerramentas
                    : ragAssistantSomenteLeitura;

            resultado = assistente.responder(conversaId, perguntaTexto);
        } finally {
            ConversaContextHolder.limpar();
            persistentChatMemoryStore.invalidar(conversaId);
        }

        List<FonteDTO> fontes = extrairFontes(resultado);

        // O turno final do modelo às vezes não traz texto algum (ex.: terminou só com uma
        // chamada de ferramenta, sem comentário) — chat_mensagem.conteudo é NOT NULL, e sem
        // este fallback a mensagem toda falhava ao salvar, derrubando a transação (inclusive
        // qualquer documento que a ferramenta tivesse acabado de criar/confirmar). O texto
        // NÃO pode sugerir sucesso de uma ação: isso vira histórico real da conversa, e o
        // próprio modelo passa a "lembrar" (erradamente) de ter concluído algo que não
        // necessariamente aconteceu, nos turnos seguintes.
        String conteudoResposta = resultado.content();
        if (conteudoResposta == null || conteudoResposta.isBlank()) {
            conteudoResposta = "(o assistente não retornou uma mensagem de texto neste turno)";
        }

        MensagemEntity resposta = new MensagemEntity();
        resposta.setConversaId(conversaId);
        resposta.setPapel(Papel.ASSISTANT);
        resposta.setConteudo(conteudoResposta);
        resposta.setFontes(serializarFontes(fontes));
        resposta.setCriadoEm(LocalDateTime.now());
        mensagemRepository.save(resposta);

        return toMensagemResponseDTO(resposta, fontes);
    }

    private ConversaEntity buscarConversaDoUsuarioOrElseThrow(UUID conversaId) {
        Long usuarioId = currentUserProvider.getCurrentUserId();

        return conversaRepository.findByIdAndUsuarioId(conversaId, usuarioId)
                .orElseThrow(() -> new NotFoundException("Conversa não encontrada com o ID: " + conversaId));
    }

    private List<FonteDTO> extrairFontes(Result<String> resultado) {
        return resultado.sources().stream()
                .map(this::toFonteDTO)
                .distinct()
                .toList();
    }

    private FonteDTO toFonteDTO(Content content) {
        Metadata metadata = content.textSegment().metadata();

        UUID documentoId = metadata.getUUID("documento_id");
        String titulo = metadata.getString("titulo");
        String secao = metadata.getString("secao");

        return new FonteDTO(documentoId, titulo, secao, "/documentos/" + documentoId + "#" + secao);
    }

    private String serializarFontes(List<FonteDTO> fontes) {
        try {
            return objectMapper.writeValueAsString(fontes);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar as fontes da resposta", e);
        }
    }

    private List<FonteDTO> desserializarFontes(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<FonteDTO>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private ConversaResponseDTO toConversaResponseDTO(ConversaEntity conversa) {
        return new ConversaResponseDTO(conversa.getId(), conversa.getTitulo(), conversa.getCriadoEm());
    }

    private MensagemResponseDTO toMensagemResponseDTO(MensagemEntity mensagem) {
        return toMensagemResponseDTO(mensagem, desserializarFontes(mensagem.getFontes()));
    }

    private MensagemResponseDTO toMensagemResponseDTO(MensagemEntity mensagem, List<FonteDTO> fontes) {
        return new MensagemResponseDTO(
                mensagem.getId(),
                mensagem.getPapel(),
                mensagem.getConteudo(),
                fontes,
                mensagem.getCriadoEm()
        );
    }
}
