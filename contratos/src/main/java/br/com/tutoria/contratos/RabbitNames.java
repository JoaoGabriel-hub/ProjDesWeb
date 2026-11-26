package br.com.tutoria.contratos;

/**
 * Nomes de todas as exchanges, filas e routing keys do RabbitMQ.
 *
 * Gateway e Worker devem SEMPRE usar estas constantes, nunca escrever
 * os nomes à mão. Um nome digitado errado não dá erro de compilação:
 * a mensagem simplesmente vai para lugar nenhum.
 *
 * Como funciona o roteamento no RabbitMQ:
 *   quem publica envia para uma EXCHANGE com uma ROUTING KEY;
 *   a exchange entrega na FILA ligada (binding) àquela routing key;
 *   quem consome escuta a FILA.
 *
 * As filas, exchanges e ligações são criadas pela MensageriaConfig,
 * também deste módulo.
 */
public final class RabbitNames {

    /*
     * PERGUNTAS — Gateway → Worker
     *
     * Parte 2 (Gateway) publica:
     *   rabbitTemplate.convertAndSend(RabbitNames.PERGUNTAS_EXCHANGE,
     *                                 RabbitNames.PERGUNTAS_ROUTING_KEY, pergunta);
     *
     * Parte 3 (Worker) consome:
     *   @RabbitListener(queues = RabbitNames.PERGUNTAS_QUEUE)
     */
    public static final String PERGUNTAS_EXCHANGE = "perguntas.exchange";
    public static final String PERGUNTAS_QUEUE = "perguntas.queue";
    public static final String PERGUNTAS_ROUTING_KEY = "pergunta.nova";

    /*
     * RESPOSTAS — Worker → Gateway
     *
     * Parte 3 (Worker) publica:
     *   rabbitTemplate.convertAndSend(RabbitNames.RESPOSTAS_EXCHANGE,
     *                                 RabbitNames.RESPOSTAS_ROUTING_KEY, resposta);
     *
     * Parte 2 (Gateway) consome e repassa ao React via SSE:
     *   @RabbitListener(queues = RabbitNames.RESPOSTAS_QUEUE)
     */
    public static final String RESPOSTAS_EXCHANGE = "respostas.exchange";
    public static final String RESPOSTAS_QUEUE = "respostas.queue";
    public static final String RESPOSTAS_ROUTING_KEY = "resposta.pronta";

    /*
     * MENSAGENS MORTAS (DLQ) — perguntas que não puderam ser processadas
     *
     * Se o Worker rejeitar uma pergunta (ex.: JSON inválido, erro inesperado),
     * o RabbitMQ a move automaticamente para a perguntas.dlq em vez de
     * descartá-la. Ninguém consome esta fila: ela serve para o grupo
     * inspecionar pelo painel o que deu errado.
     */
    public static final String PERGUNTAS_DLX = "perguntas.dlx";
    public static final String PERGUNTAS_DLQ = "perguntas.dlq";
    public static final String PERGUNTAS_DLQ_ROUTING_KEY = "pergunta.falhou";

    /** Classe só de constantes: não pode ser instanciada. */
    private RabbitNames() {
    }
}