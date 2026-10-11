package br.com.example.senac.businessDocsAi.document.event;

import java.util.UUID;

/**
 * Publicado após o commit da tool leve de solicitação de geração estruturada (Etapa 13.3).
 * Só leva o ID do rascunho — todo o resto que o worker precisa (conversaId, tipo,
 * documentoIdAlvo, categoriaId, turnoCriacao, instrucoesAdicionais) já está em colunas do
 * próprio {@code RascunhoDocumentoEntity} (R2: nada viaja por ThreadLocal/SecurityContext, o
 * worker roda fora da requisição).
 */
public record GeracaoEstruturadaSolicitadaEvent(UUID rascunhoId) {
}
