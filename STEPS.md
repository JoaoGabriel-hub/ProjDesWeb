# Mensageria: como uma pergunta percorre o sistema

Este arquivo explica o módulo `contratos` e como o Gateway e o Worker devem usá-lo. Para o setup da máquina, veja o [`INSTRUCOES.md`](INSTRUCOES.md).

---

## 1. O fluxo, em resumo

```
React ─▶ Gateway ─▶ [perguntas.queue] ─▶ Worker ─▶ LLM
                                            │
React ◀─ Gateway ◀─ [respostas.queue] ◀─────┘
```

1. O **React** envia o texto da pergunta ao Gateway.
2. O **Gateway** monta a `PerguntaMessage` e a publica na `perguntas.queue`.
3. O **Worker** pega a pergunta, acrescenta a instrução de "tutor acadêmico" e chama a LLM.
4. O **Worker** monta a `RespostaMessage` e a publica na `respostas.queue`.
5. O **Gateway** pega a resposta e a envia, pelo SSE, **só para o usuário que perguntou**.

A conversão das mensagens para JSON, e de volta, é automática nos dois lados.

---

## 2. O que o módulo `contratos` entrega

| Classe | Para quê |
|---|---|
| `PerguntaMessage` | A pergunta que vai do Gateway para o Worker |
| `RespostaMessage` | A resposta que vai do Worker para o Gateway |
| `RabbitNames` | Os nomes das filas e exchanges. **Nunca escreva esses nomes à mão** |
| `MensageriaConfig` | Cria as filas no RabbitMQ e configura a conversão para JSON |

As filas são criadas **automaticamente** quando o Gateway ou o Worker sobe. Não é preciso criar nada pelo painel.

---

## 3. Como usar (Gateway e Worker)

**a) Adicione a dependência** no `pom.xml` do seu projeto:

```xml
<dependency>
    <groupId>br.com.tutoria</groupId>
    <artifactId>contratos</artifactId>
    <version>${project.version}</version>
</dependency>
```

**b) Importe a configuração** na classe principal:

```java
@SpringBootApplication
@Import(MensageriaConfig.class)
public class GatewayApplication { ... }
```

**c) Configure a conexão** no `application.yml`:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: desweb
    password: desweb
```

**d) Antes de rodar pela primeira vez**, na raiz do repositório:

```bash
./mvnw install -DskipTests
```

---

## 4. O que cada um faz

### Gateway (Parte 2)

- **Ao receber uma pergunta do React,** o Gateway monta uma `PerguntaMessage`. Ele gera um identificador único para a pergunta (o `correlationId`), preenche quem perguntou com o usuário do token JWT, coloca o texto digitado e a data e hora atuais.
- **Em seguida, publica essa mensagem** na exchange de perguntas, usando os nomes que estão na `RabbitNames`. A conversão para JSON é automática.
- **O Gateway também escuta a fila de respostas.** Quando chega uma `RespostaMessage`, ele olha quem fez a pergunta e envia a resposta só para essa pessoa, pela conexão SSE dela.

### Worker (Parte 3)

- **O Worker escuta a fila de perguntas.** Cada pergunta que chega já vem convertida em `PerguntaMessage`, pronta para ser lida.
- **Ele acrescenta a instrução de "tutor acadêmico"** antes do texto da pergunta e chama a LLM. Se a LLM falhar, espera um pouco e tenta de novo.
- **Para montar a resposta,** o Worker usa sempre um dos dois atalhos da `RespostaMessage`: `sucesso`, quando a LLM respondeu, ou `falha`, quando todas as tentativas deram errado. Os dois copiam automaticamente o identificador e o usuário da pergunta, para a resposta chegar à pessoa certa.
- **Por fim, publica a resposta** na exchange de respostas, usando os nomes da `RabbitNames`.
- **No `application.yml` do Worker,** a propriedade `spring.rabbitmq.listener.simple.default-requeue-rejected` deve ficar como `false`. Sem isso, uma pergunta que der erro volta para a fila infinitamente, em vez de ir para a DLQ.

---

## 5. Formato das mensagens

| `PerguntaMessage` | `RespostaMessage` |
|---|---|
| `correlationId`: identificador único (UUID) | `correlationId`: o mesmo da pergunta |
| `userId`: quem perguntou | `userId`: o mesmo da pergunta |
| `pergunta`: texto digitado | `resposta`: texto da LLM (`null` se falhou) |
| `criadaEm`: data e hora | `sucesso`: `true` ou `false` |
| | `erro`: mensagem de erro (`null` se deu certo) |
| | `respondidaEm`: data e hora |

---

## 6. As filas

| Fila | Quem publica | Quem consome |
|---|---|---|
| `perguntas.queue` | Gateway | Worker |
| `respostas.queue` | Worker | Gateway |
| `perguntas.dlq` | o próprio RabbitMQ | ninguém: serve para ver no painel as perguntas que deram erro |

Todas são **duráveis**: as mensagens não se perdem se o RabbitMQ reiniciar.