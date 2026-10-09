package br.com.example.senac.businessDocsAi.document.entity;

public enum StatusRascunho {
    PENDENTE,
    CONFIRMADO,
    DESCARTADO,
    // Geração assíncrona (Etapa 13.1) — só usados pelo caminho novo da tool estruturada; o
    // fluxo legado (HTML síncrono) nunca entra nestes dois estados.
    GERANDO,
    ERRO_GERACAO
}
