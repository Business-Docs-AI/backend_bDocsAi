package br.com.example.senac.businessDocsAi.document.entity;

import java.util.List;

public enum StatusRascunho {
    PENDENTE,
    CONFIRMADO,
    DESCARTADO,
    // Geração assíncrona (Etapa 13.1) — só usados pelo caminho novo da tool estruturada; o
    // fluxo legado (HTML síncrono) nunca entra nestes dois estados.
    GERANDO,
    ERRO_GERACAO;

    // R1: "proposta ativa" de uma conversa — a mesma regra de sempre ("só uma proposta
    // ativa por vez, a mais recente colapsa nela") agora olha os 3 estados em que um
    // rascunho ainda importa pra conversa. CONFIRMADO/DESCARTADO nunca contam como ativos.
    public static List<StatusRascunho> ativos() {
        return List.of(PENDENTE, GERANDO, ERRO_GERACAO);
    }
}
