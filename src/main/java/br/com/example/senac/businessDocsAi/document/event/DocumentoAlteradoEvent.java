package br.com.example.senac.businessDocsAi.document.event;

import java.util.UUID;

/**
 * Publicado após o commit de uma criação/edição/restauração de documento.
 * O listener de indexação confere se {@code versao} ainda é a vigente antes de indexar,
 * para uma indexação atrasada nunca sobrescrever uma versão mais nova.
 */
public record DocumentoAlteradoEvent(UUID documentoId, int versao) {
}
