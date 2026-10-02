package br.com.example.senac.businessDocsAi.chat.controller;

import br.com.example.senac.businessDocsAi.chat.dto.ConversaResponseDTO;
import br.com.example.senac.businessDocsAi.chat.dto.CriarConversaRequestDTO;
import br.com.example.senac.businessDocsAi.chat.dto.MensagemRequestDTO;
import br.com.example.senac.businessDocsAi.chat.dto.MensagemResponseDTO;
import br.com.example.senac.businessDocsAi.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat/conversas")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversaResponseDTO> criar(
            @RequestBody(required = false) CriarConversaRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.criarConversa(dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversaResponseDTO>> listar() {
        return ResponseEntity.ok(chatService.listarConversas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MensagemResponseDTO>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(chatService.buscarConversa(id));
    }

    @PostMapping("/{id}/mensagens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MensagemResponseDTO> enviarMensagem(
            @PathVariable UUID id,
            @Valid @RequestBody MensagemRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.enviarMensagem(id, dto));
    }

    // Mesma rota, mas para quando o front envia áudio e/ou um documento anexado junto (ou
    // em vez de) um texto — por isso um método HTTP separado em vez de um campo extra no
    // MensagemRequestDTO, já que corpo multipart e JSON não se misturam na mesma requisição.
    @PostMapping(value = "/{id}/mensagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MensagemResponseDTO> enviarMensagemComArquivo(
            @PathVariable UUID id,
            @RequestParam(value = "pergunta", required = false) String pergunta,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "anexo", required = false) MultipartFile anexo
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.enviarMensagemComArquivo(id, pergunta, audio, anexo));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        chatService.excluirConversa(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/exportar", produces = "text/markdown; charset=UTF-8")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> exportar(@PathVariable UUID id) {
        String markdown = chatService.exportarConversaMarkdown(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"conversa-" + id + ".md\"")
                .body(markdown);
    }
}
