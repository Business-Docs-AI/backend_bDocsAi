package br.com.example.senac.businessDocsAi.chat.service;

import br.com.example.senac.businessDocsAi.ai.generation.RagAssistant;
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

    @Qualifier("ragAssistantSomenteLeitura")
    private final RagAssistant ragAssistantSomenteLeitura;

    @Qualifier("ragAssistantComFerramentas")
    private final RagAssistant ragAssistantComFerramentas;

    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

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

    // Apaga tudo que pertence à conversa: mensagens e também os rascunhos de documento
    // propostos nela (confirmados ou não) — nada da conversa sobrevive à sua exclusão. O
    // documento em si, se a proposta já tiver sido confirmada antes da exclusão, não é
    // afetado: ele é uma entidade independente a partir daí.
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void excluirConversa(UUID conversaId) {

        ConversaEntity conversa = buscarConversaDoUsuarioOrElseThrow(conversaId);

        rascunhoRepository.deleteByConversaId(conversaId);
        mensagemRepository.deleteByConversaId(conversaId);
        conversaRepository.delete(conversa);
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

        MensagemEntity pergunta = new MensagemEntity();
        pergunta.setConversaId(conversaId);
        pergunta.setPapel(Papel.USER);
        pergunta.setConteudo(dto.pergunta());
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

            resultado = assistente.responder(conversaId, dto.pergunta());
        } finally {
            ConversaContextHolder.limpar();
        }

        List<FonteDTO> fontes = extrairFontes(resultado);

        MensagemEntity resposta = new MensagemEntity();
        resposta.setConversaId(conversaId);
        resposta.setPapel(Papel.ASSISTANT);
        resposta.setConteudo(resultado.content());
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
