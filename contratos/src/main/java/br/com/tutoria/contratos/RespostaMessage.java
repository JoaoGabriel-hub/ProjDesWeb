package br.com.tutoria.contratos;

import java.time.Instant;

/**
 * Mensagem que representa a RESPOSTA (ou a falha) para uma pergunta.
 *
 * Fluxo: Worker → [respostas.queue] → Gateway → SSE → React
 *
 * - Parte 3 (Worker): CRIA esta mensagem depois de chamar a LLM, sempre
 *   pelos métodos sucesso() ou falha() abaixo, e publica na fila de respostas.
 * - Parte 2 (Gateway): LÊ esta mensagem no @RabbitListener da fila de
 *   respostas, usa o userId para achar o SseEmitter do usuário certo e
 *   envia a mensagem para o React.
 * - Parte 4 (React): RECEBE como JSON pelo SSE, usa o correlationId para
 *   saber a qual pergunta ela pertence e o campo sucesso para decidir se
 *   mostra a resposta ou o erro.
 * - Parte 1 (contratos): DEFINE o formato.
 */
public record RespostaMessage(
        String correlationId,
        String userId,
        String resposta,
        boolean sucesso,
        String erro,
        Instant respondidaEm
) {

    /**
     * RESPOSTA DE SUCESSO: a LLM respondeu normalmente.
     *
     * Usado pela Parte 3 (Worker):
     *   RespostaMessage.sucesso(pergunta, textoDaLlm);
     *
     * Copia o correlationId e o userId da pergunta original, para a
     * resposta chegar ao usuário certo e aparecer embaixo da pergunta
     * certa no React.
     */
    public static RespostaMessage sucesso(PerguntaMessage pergunta, String resposta) {
        return new RespostaMessage(
                pergunta.correlationId(),
                pergunta.userId(),
                resposta,
                true,
                null,
                Instant.now()
        );
    }

    /**
     * RESPOSTA DE FALHA: a LLM falhou mesmo depois das novas tentativas
     * (retry do passo 1.5 da especificação).
     *
     * Usado pela Parte 3 (Worker):
     *   RespostaMessage.falha(pergunta, "Serviço de IA indisponível");
     *
     * O Worker publica esta resposta normalmente. O Gateway entrega ao
     * React, que mostra o erro ao usuário em vez de ficar carregando para
     * sempre. Assim, uma falha externa não quebra a experiência do usuário.
     */
    public static RespostaMessage falha(PerguntaMessage pergunta, String erro) {
        return new RespostaMessage(
                pergunta.correlationId(),
                pergunta.userId(),
                null,
                false,
                erro,
                Instant.now()
        );
    }
}