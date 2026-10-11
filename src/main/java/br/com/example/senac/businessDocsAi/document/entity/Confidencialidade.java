package br.com.example.senac.businessDocsAi.document.entity;

/**
 * Metadado de confidencialidade do documento. Nesta entrega é só metadado gravado (nenhuma
 * regra de acesso nova é aplicada por confidencialidade — isso fica para uma entrega futura,
 * a ser consultada com o usuário antes de implementar).
 */
public enum Confidencialidade {
    PUBLICO,
    INTERNO,
    CONFIDENCIAL,
    RESTRITO
}
