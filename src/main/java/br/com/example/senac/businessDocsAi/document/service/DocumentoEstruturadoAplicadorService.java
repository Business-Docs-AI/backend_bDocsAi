package br.com.example.senac.businessDocsAi.document.service;

import br.com.example.senac.businessDocsAi.document.dto.DocumentoEstruturadoMetadadosDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoRequestDTO;
import br.com.example.senac.businessDocsAi.document.dto.DocumentoResponseDTO;
import br.com.example.senac.businessDocsAi.document.dto.estruturado.DocumentoEstruturadoDTO;
import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.TipoRascunho;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aplica a confirmação de uma proposta de documento ESTRUTURADO (Etapa 13.3) — ponte entre
 * o rascunho (cujo {@code conteudoEstruturado} guarda conteúdo E metadados juntos, por ser
 * só uma proposta — ver comentário em {@code RascunhoDocumentoEntity}) e
 * {@code DocumentoService} (cujas colunas de metadado e {@code conteudo_estruturado} do
 * documento real são estritamente separados — decisão B3).
 */
@Service
@RequiredArgsConstructor
public class DocumentoEstruturadoAplicadorService {

    private final ObjectMapper objectMapper;
    private final DocumentoService documentoService;

    public DocumentoResponseDTO aplicar(RascunhoDocumentoEntity rascunho) {
        DocumentoEstruturadoDTO estrutura = desserializar(rascunho.getConteudoEstruturado());

        String conteudoJson = serializarSoConteudo(estrutura);
        DocumentoEstruturadoMetadadosDTO metadados = extrairMetadados(estrutura);

        DocumentoRequestDTO dto = new DocumentoRequestDTO(
                rascunho.getTitulo(), rascunho.getConteudoHtml(), "Gerado via chat com IA (documento estruturado)",
                rascunho.getCategoriaId()
        );

        return rascunho.getTipo() == TipoRascunho.CRIAR
                ? documentoService.criarComEstrutura(
                        dto, conteudoJson, DocumentoEstruturadoDTO.VERSAO_SCHEMA_ATUAL, metadados
                )
                : documentoService.atualizarComEstrutura(
                        rascunho.getDocumentoIdAlvo(), dto, conteudoJson,
                        DocumentoEstruturadoDTO.VERSAO_SCHEMA_ATUAL, metadados
                );
    }

    private DocumentoEstruturadoDTO desserializar(String json) {
        try {
            return objectMapper.readValue(json, DocumentoEstruturadoDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("conteudo_estruturado do rascunho não é um JSON válido", e);
        }
    }

    // Decisão B3: só os campos de CONTEÚDO vão para o conteudo_estruturado salvo em
    // documento/documento_versao — nunca o bloco de metadados (que vira colunas, ver
    // extrairMetadados). Usa um Map em vez de um record novo pra não duplicar a lista de
    // campos de conteúdo em outro lugar além deste método.
    private String serializarSoConteudo(DocumentoEstruturadoDTO e) {
        Map<String, Object> conteudo = new LinkedHashMap<>();
        conteudo.put("titulo", e.titulo());
        conteudo.put("tipoDocumento", e.tipoDocumento());
        conteudo.put("objetivo", e.objetivo());
        conteudo.put("escopo", e.escopo());
        conteudo.put("gatilho", e.gatilho());
        conteudo.put("raci", e.raci());
        conteudo.put("fluxo", e.fluxo());
        conteudo.put("sipoc", e.sipoc());
        conteudo.put("regrasNegocio", e.regrasNegocio());
        conteudo.put("excecoes", e.excecoes());
        conteudo.put("sistemasFerramentas", e.sistemasFerramentas());
        conteudo.put("riscosControles", e.riscosControles());
        conteudo.put("indicadores", e.indicadores());
        conteudo.put("glossario", e.glossario());
        conteudo.put("documentosRelacionados", e.documentosRelacionados());
        conteudo.put("pendencias", e.pendencias());

        try {
            return objectMapper.writeValueAsString(conteudo);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar o conteúdo estruturado", ex);
        }
    }

    private DocumentoEstruturadoMetadadosDTO extrairMetadados(DocumentoEstruturadoDTO e) {
        UUID processoPaiId = null;
        if (e.processoPaiId() != null && !e.processoPaiId().isBlank()) {
            try {
                processoPaiId = UUID.fromString(e.processoPaiId().trim());
            } catch (IllegalArgumentException ex) {
                // ID de processo pai inválido vindo do modelo — ignora em vez de falhar a
                // confirmação inteira por causa de um metadado secundário.
                processoPaiId = null;
            }
        }

        return new DocumentoEstruturadoMetadadosDTO(
                e.tipoDocumento(), e.macroprocessoId(), processoPaiId, e.donoProcesso(), e.aprovador(),
                e.periodicidadeRevisaoMeses(), e.confidencialidade(), e.tags(), e.areasParticipantes()
        );
    }
}
