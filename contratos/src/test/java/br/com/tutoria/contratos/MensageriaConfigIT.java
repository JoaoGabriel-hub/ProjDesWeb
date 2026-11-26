package br.com.tutoria.contratos;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Teste de integração da MensageriaConfig com um RabbitMQ real.
 *
 * Termina em "IT" para NÃO rodar no ./mvnw package (que quebraria sem o
 * RabbitMQ ligado). Para rodar, com o RabbitMQ ligado:
 *   ./mvnw -pl contratos test -Dtest=MensageriaConfigIT
 *
 * Depois de rodar, as filas e exchanges aparecem no painel do RabbitMQ.
 */
@SpringBootTest
class MensageriaConfigIT {

    /** Simula uma aplicação (como o Gateway ou o Worker) que importa a MensageriaConfig. */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(MensageriaConfig.class)
    static class AplicacaoDeTeste {
    }

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void deveCriarTodasAsFilasNoRabbitMQ() {
        ((RabbitAdmin) amqpAdmin).initialize();

        assertNotNull(amqpAdmin.getQueueProperties(RabbitNames.PERGUNTAS_QUEUE));
        assertNotNull(amqpAdmin.getQueueProperties(RabbitNames.RESPOSTAS_QUEUE));
        assertNotNull(amqpAdmin.getQueueProperties(RabbitNames.PERGUNTAS_DLQ));
    }

    @Test
    void devePublicarEConsumirUmaPerguntaEmJson() {
        PerguntaMessage enviada = new PerguntaMessage(
                UUID.randomUUID().toString(),
                "usuario-teste",
                "O que é escopo?",
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                RabbitNames.PERGUNTAS_EXCHANGE,
                RabbitNames.PERGUNTAS_ROUTING_KEY,
                enviada
        );

        Object recebida = rabbitTemplate.receiveAndConvert(RabbitNames.PERGUNTAS_QUEUE, 5000);

        PerguntaMessage pergunta = assertInstanceOf(PerguntaMessage.class, recebida);
        assertEquals(enviada.correlationId(), pergunta.correlationId());
        assertEquals(enviada.pergunta(), pergunta.pergunta());
    }
}