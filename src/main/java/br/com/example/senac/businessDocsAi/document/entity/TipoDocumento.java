package br.com.example.senac.businessDocsAi.document.entity;

/**
 * Tipo do documento na pirâmide documental (ISO 9001). Documentos criados antes desta
 * entrega ficam como {@link #NAO_CLASSIFICADO} (ver migration V9) — oferecer a
 * classificação depois é responsabilidade do frontend, nunca forçada automaticamente.
 */
public enum TipoDocumento {
    POLITICA,
    PROCESSO,
    PROCEDIMENTO,
    INSTRUCAO,
    REGISTRO,
    NAO_CLASSIFICADO
}
