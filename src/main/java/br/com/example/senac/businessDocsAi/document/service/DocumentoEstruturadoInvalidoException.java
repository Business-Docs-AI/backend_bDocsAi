package br.com.example.senac.businessDocsAi.document.service;

import java.util.List;

/**
 * Lançada por {@link GeracaoEstruturadaService} quando o modelo não produz um documento
 * estruturado válido (Bean Validation + {@link DocumentoEstruturadoValidator}) mesmo depois
 * do laço de correção dentro da mesma tentativa (decisão A2, adaptada ao worker).
 */
public class DocumentoEstruturadoInvalidoException extends RuntimeException {

    private final List<String> erros;

    public DocumentoEstruturadoInvalidoException(List<String> erros) {
        super("Documento estruturado inválido: " + String.join("; ", erros));
        this.erros = erros;
    }

    public List<String> getErros() {
        return erros;
    }
}
