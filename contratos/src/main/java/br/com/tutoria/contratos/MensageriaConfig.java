package br.com.tutoria.contratos;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ compartilhada entre Gateway e Worker.
 *
 * Esta classe é a "receita" de tudo o que precisa existir no RabbitMQ:
 * exchanges, filas, ligações entre elas (bindings) e o conversor JSON.
 * Quando uma aplicação Spring que importa esta classe se conecta ao
 * RabbitMQ, o Spring cria automaticamente tudo o que ainda não existe.
 * Por isso não é preciso criar nada à mão pelo painel.
 *
 * COMO USAR (Parte 2 - Gateway e Parte 3 - Worker):
 *   na classe principal da aplicação (a que tem @SpringBootApplication), adicione:
 *
 *     @Import(MensageriaConfig.class)
 *
 *   O @Import é necessário porque esta classe está em outro pacote
 *   (br.com.tutoria.contratos), que o Spring não encontraria sozinho.
 *
 * Requisitos da especificação atendidos aqui:
 *   - Persistência de mensagens: filas e exchanges duráveis.
 *   - Serialização: o mesmo conversor Jackson nos dois lados.
 *   - Resiliência: fila de mensagens mortas (DLQ) para perguntas
 *     que não puderam ser processadas.
 *
 * ATENÇÃO: depois que uma fila é criada no RabbitMQ, mudar a configuração
 * dela aqui (ex.: adicionar ou remover a DLQ) causa erro
 * PRECONDITION_FAILED ao subir a aplicação. Nesse caso, apague a fila
 * pelo painel (aba Queues) e suba a aplicação de novo.
 */
@Configuration
public class MensageriaConfig {

    // ======================= CONVERSOR JSON =======================

    /** Usado por: Gateway e Worker, automaticamente, ao publicar (RabbitTemplate) e ao consumir (@RabbitListener). */
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("br.com.tutoria.contratos");
    }

    // ================= PERGUNTAS — Gateway → Worker =================

    /** Usado por: Gateway (Parte 2), para publicar as perguntas no POST /perguntas. */
    @Bean
    public DirectExchange perguntasExchange() {
        return ExchangeBuilder
                .directExchange(RabbitNames.PERGUNTAS_EXCHANGE)
                .durable(true)
                .build();
    }

    /** Usado por: Worker (Parte 3), no @RabbitListener que consome as perguntas. */
    @Bean
    public Queue perguntasQueue() {
        return QueueBuilder
                .durable(RabbitNames.PERGUNTAS_QUEUE)
                .deadLetterExchange(RabbitNames.PERGUNTAS_DLX)
                .deadLetterRoutingKey(RabbitNames.PERGUNTAS_DLQ_ROUTING_KEY)
                .build();
    }

    /** Liga a perguntas.exchange à perguntas.queue. Ninguém usa diretamente. */
    @Bean
    public Binding perguntasBinding() {
        return BindingBuilder
                .bind(perguntasQueue())
                .to(perguntasExchange())
                .with(RabbitNames.PERGUNTAS_ROUTING_KEY);
    }

    // ================= RESPOSTAS — Worker → Gateway =================

    /** Usado por: Worker (Parte 3), para publicar as respostas depois de chamar a LLM. */
    @Bean
    public DirectExchange respostasExchange() {
        return ExchangeBuilder
                .directExchange(RabbitNames.RESPOSTAS_EXCHANGE)
                .durable(true)
                .build();
    }

    /** Usado por: Gateway (Parte 2), no @RabbitListener que repassa as respostas ao React via SSE. */
    @Bean
    public Queue respostasQueue() {
        return QueueBuilder
                .durable(RabbitNames.RESPOSTAS_QUEUE)
                .build();
    }

    /** Liga a respostas.exchange à respostas.queue. Ninguém usa diretamente. */
    @Bean
    public Binding respostasBinding() {
        return BindingBuilder
                .bind(respostasQueue())
                .to(respostasExchange())
                .with(RabbitNames.RESPOSTAS_ROUTING_KEY);
    }

    // ============ MENSAGENS MORTAS (DLQ) — perguntas rejeitadas ============

    /** Usado por: o próprio RabbitMQ, quando o Worker rejeita uma pergunta. */
    @Bean
    public DirectExchange perguntasDlx() {
        return ExchangeBuilder
                .directExchange(RabbitNames.PERGUNTAS_DLX)
                .durable(true)
                .build();
    }

    /** Usado por: o grupo, pelo painel do RabbitMQ, para inspecionar as perguntas que falharam. */
    @Bean
    public Queue perguntasDlq() {
        return QueueBuilder
                .durable(RabbitNames.PERGUNTAS_DLQ)
                .build();
    }

    /** Liga a perguntas.dlx (exchange para onde o RabbitMQ manda as perguntas que o Worker rejeitou) à perguntas.dlq (file das perguntas rejeitadas). Ninguém usa diretamente. */
    @Bean
    public Binding perguntasDlqBinding() {
        return BindingBuilder
                .bind(perguntasDlq())
                .to(perguntasDlx())
                .with(RabbitNames.PERGUNTAS_DLQ_ROUTING_KEY);
    }
}