package br.com.example.senac.businessDocsAi.document.repository;

import br.com.example.senac.businessDocsAi.document.entity.RascunhoDocumentoEntity;
import br.com.example.senac.businessDocsAi.document.entity.StatusRascunho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IRascunhoDocumentoRepository extends JpaRepository<RascunhoDocumentoEntity, UUID> {

    Optional<RascunhoDocumentoEntity> findFirstByConversaIdAndStatusOrderByCriadoEmDesc(
            UUID conversaId, StatusRascunho status
    );

    // Geração assíncrona (R1): "proposta ativa" da conversa agora pode estar em PENDENTE
    // (fluxo legado/já gerado), GERANDO ou ERRO_GERACAO (fluxo novo) — mesma regra de "só
    // uma proposta ativa por conversa" de sempre, agora olhando os 3 estados que contam como
    // ativos (DESCARTADO/CONFIRMADO nunca contam).
    Optional<RascunhoDocumentoEntity> findFirstByConversaIdAndStatusInOrderByCriadoEmDesc(
            UUID conversaId, Collection<StatusRascunho> status
    );

    List<RascunhoDocumentoEntity> findByStatusOrderByCriadoEmAsc(StatusRascunho status);

    void deleteByConversaId(UUID conversaId);

    // R3: reserva atômica antes de processar — só "ganha" quem conseguir mudar a linha
    // (nenhum SELECT prévio decide por conta própria). Elegível quando: ainda está GERANDO,
    // não foi reservado ainda OU a reserva anterior expirou (worker anterior travou/caiu), e
    // não esgotou o limite de tentativas. affected=0 significa "outro worker já reservou" ou
    // "não está mais elegível" (ex.: usuário descartou) — o chamador deve desistir em
    // silêncio, nunca tratar como erro.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RascunhoDocumentoEntity r
               SET r.reservadoEm = :agora, r.tentativasGeracao = r.tentativasGeracao + 1
             WHERE r.id = :id
               AND r.status = 'GERANDO'
               AND r.tentativasGeracao < :maxTentativas
               AND (r.reservadoEm IS NULL OR r.reservadoEm < :expiradoAntesDe)
            """)
    int reservarParaProcessamento(
            @Param("id") UUID id,
            @Param("agora") LocalDateTime agora,
            @Param("expiradoAntesDe") LocalDateTime expiradoAntesDe,
            @Param("maxTentativas") int maxTentativas
    );

    // R1: só grava o sucesso se o rascunho AINDA estiver GERANDO — se o usuário descartou no
    // meio (status virou DESCARTADO), affected=0 e o resultado deve ser descartado em
    // silêncio pelo chamador, nunca sobrescrever o DESCARTADO.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RascunhoDocumentoEntity r
               SET r.status = 'PENDENTE',
                   r.titulo = :titulo,
                   r.conteudoHtml = :conteudoHtml,
                   r.conteudoEstruturado = :conteudoEstruturado,
                   r.versaoSchema = :versaoSchema,
                   r.categoriaId = :categoriaId
             WHERE r.id = :id
               AND r.status = 'GERANDO'
            """)
    int finalizarComSucesso(
            @Param("id") UUID id,
            @Param("titulo") String titulo,
            @Param("conteudoHtml") String conteudoHtml,
            @Param("conteudoEstruturado") String conteudoEstruturado,
            @Param("versaoSchema") String versaoSchema,
            @Param("categoriaId") Long categoriaId
    );

    // R1: mesma proteção de concorrência da finalização de sucesso.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RascunhoDocumentoEntity r
               SET r.status = 'ERRO_GERACAO', r.erroGeracao = :erroGeracao
             WHERE r.id = :id
               AND r.status = 'GERANDO'
            """)
    int finalizarComErro(@Param("id") UUID id, @Param("erroGeracao") String erroGeracao);
}
