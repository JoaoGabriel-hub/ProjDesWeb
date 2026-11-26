package br.com.tutoria.contratos;

import java.time.Instant;

/**
 * Mensagem que representa UMA PERGUNTA feita por um usuário.
 *
 * Fluxo: React → Gateway → [perguntas.queue] → Worker
 *
 * - Parte 2 (Gateway): CRIA esta mensagem no endpoint POST /perguntas,
 *   gerando o correlationId (UUID), preenchendo o userId com o usuário
 *   autenticado pelo JWT, e publica na fila de perguntas.
 * - Parte 3 (Worker): LÊ esta mensagem no @RabbitListener da fila de
 *   perguntas, aplica a máscara de tutor acadêmico ao texto e chama a LLM.
 * - Parte 1 (contratos): DEFINE o formato. Qualquer mudança afeta o
 *   Gateway e o Worker, então só alterar combinando com o grupo.
 *
 * É um "record": o Java gera o construtor e os getters
 * (ex.: pergunta.correlationId()), e o Jackson converte para JSON
 * automaticamente.
 */
public record PerguntaMessage(
        String correlationId,
        String userId,
        String pergunta,
        Instant criadaEm
) {
}