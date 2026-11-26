# Sistema de Tutoria Assíncrono com IA

Sistema distribuído de perguntas e respostas: o usuário envia uma pergunta pelo React, o Gateway publica a pergunta no RabbitMQ, um Worker consulta a LLM (via Spring AI) e a resposta volta ao navegador em tempo real por SSE.

```
React ──HTTP──▶ Gateway ──▶ perguntas.queue ──▶ Worker ──▶ LLM (Ollama)
  ▲                                                 │
  └──SSE── Gateway ◀── respostas.queue ◀──────────┘
```

**Integrantes:** João, David, Felipe, Matheus

---

## Estrutura do repositório

| Pasta | O que é | Parte | Responsável |
|---|---|---|---|
| `contratos/` | DTOs das mensagens, nomes das filas e configuração do RabbitMQ compartilhada | 1 | João |
| `gateway/` | Spring Boot Web: endpoints REST, SSE e login | 2 (segurança: Parte 1) | Felipe |
| `worker/` | Spring Boot sem Web: consome perguntas e chama a LLM | 3 | Matheus |
| `frontend/` | React (Vite) | 4 | David |
| `pom.xml` (raiz) | Versões compartilhadas de Java, Spring Boot e Spring AI | 1 | João |
| `docker-compose.yml` | RabbitMQ e Ollama | 1 | João |

### Cada projeto tem o seu próprio `pom.xml`

O repositório tem **quatro arquivos `pom.xml`**: um na raiz e um dentro de cada projeto Java.

```
dev-web/
├── pom.xml              ← pom PAI (raiz): só define as versões e lista os módulos
├── contratos/
│   └── pom.xml          ← pom do contratos (Parte 1)
├── gateway/
│   └── pom.xml          ← pom do Gateway (Parte 2)
├── worker/
│   └── pom.xml          ← pom do Worker (Parte 3)
└── frontend/
    └── package.json     ← o front-end usa npm, não tem pom.xml
```

| | `pom.xml` da raiz (pai) | `pom.xml` de cada projeto (filho) |
|---|---|---|
| **Quem edita** | João (Parte 1) | O responsável pelo projeto |
| **O que define** | Versão do Java, do Spring Boot e do Spring AI, e a lista de módulos | As **dependências** do projeto (Web, RabbitMQ, Ollama etc.) e o nome do projeto |
| **Gera um programa?** | Não, só organiza | Sim: cada um gera o seu `.jar` |

Cada responsável cuida das dependências do **seu** `pom.xml` livremente, sem precisar pedir ao João. O `pom.xml` filho só precisa apontar para o pai no `<parent>` (veja o passo 3 abaixo), e assim herda as versões automaticamente.

**Por que um `pom.xml` pai?** O Gateway e o Worker precisam usar exatamente as mesmas classes de mensagem (o módulo `contratos`) e as mesmas versões do Spring. Com um `pom.xml` pai, as versões são definidas em um lugar só e todos os projetos as herdam. Assim não existe o "na minha máquina funciona".

---

## Pré-requisitos

| Ferramenta | Versão | Para quê | Como conferir |
|---|---|---|---|
| Java (JDK) | **21** | Gateway, Worker e contratos | `java -version` |
| Docker + Docker Compose | recente | Subir o RabbitMQ e o Ollama | `docker compose version` |
| Node.js | 20 ou mais | Front-end React | `node -v` |
| Maven | não precisa | O repositório já tem o Maven Wrapper (`./mvnw`) | — |

> O Spring Boot 4 e o Spring AI 2 **exigem Java 21**. Com o Java 8 ou o 17, o projeto não compila.
>
> No Ubuntu: `sudo apt install openjdk-21-jdk` e depois `sudo update-alternatives --config java`.

---

## Como subir o RabbitMQ e o Ollama (Docker)

Na raiz do repositório:

```bash
docker compose up -d            # sobe RabbitMQ + Ollama
docker compose up -d rabbitmq   # sobe só o RabbitMQ
docker compose ps               # confere se estão "running"
docker compose down             # para tudo (os dados continuam salvos)
```

| Serviço | Endereço | Usado por |
|---|---|---|
| RabbitMQ (AMQP) | `localhost:5672` | Gateway e Worker |
| Painel do RabbitMQ | http://localhost:15672 | Todos, para depurar |
| Ollama (API da LLM) | `localhost:11434` | Worker |

**Usuário e senha do RabbitMQ:** `desweb` / `desweb` (no painel e no código).

**Configuração no `application.yml`** do Gateway e do Worker:

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: desweb
    password: desweb
```

**No Worker**, o Spring AI precisa apontar para o Ollama:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
```

**Baixar o modelo da LLM** (apenas na primeira vez, cerca de 1,3 GB):

```bash
docker exec -it tutoria-ollama ollama pull llama3.2:1b
```

> ⚠️ **Atenção ao espaço em disco:** a imagem do Ollama ocupa cerca de 9 GB. Tenha pelo menos **10 GB livres** antes de rodar `docker compose up -d`. Se a sua máquina tiver pouco espaço ou pouca memória, suba só o RabbitMQ (`docker compose up -d rabbitmq`) e deixe o Ollama rodando na máquina de quem cuida do Worker.

## Como criar o seu projeto (Gateway ou Worker)

### 1. Gerar o projeto no [start.spring.io](https://start.spring.io)

| Campo | Valor |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.1.x (a mesma linha do `pom.xml` da raiz) |
| Group | `br.com.tutoria` |
| Artifact | `gateway` ou `worker` |
| Packaging | Jar |
| Java | 21 |

**Dependências:**

- **Gateway:** `Spring Web` e `Spring for RabbitMQ`.
  **Não adicione `Spring Security` agora.** Ele bloqueia todos os endpoints com uma senha gerada automaticamente. A segurança com JWT será adicionada pela Parte 1, e até lá os endpoints ficam abertos para facilitar o desenvolvimento.
- **Worker:** `Spring for RabbitMQ` e `Ollama` (da seção Spring AI).
  **Não adicione `Spring Web`.** A especificação exige que o Worker seja um projeto sem servidor web.

### 2. Colocar os arquivos na pasta certa

Descompacte o `.zip` e mova o **conteúdo** da pasta gerada para dentro de `gateway/` ou `worker/`. O `pom.xml` deve ficar em `gateway/pom.xml`, e não em `gateway/gateway/pom.xml`.

Depois, **apague** estes arquivos do seu projeto, porque a raiz já tem os seus:

- `mvnw`, `mvnw.cmd` e a pasta `.mvn/`, pois usamos o wrapper da raiz
- o `.gitkeep`

### 3. Ajustar o `pom.xml` do seu projeto (`gateway/pom.xml` ou `worker/pom.xml`, e não o da raiz)

**a) Troque o `<parent>`** gerado pelo Initializr por este:

```xml
<parent>
    <groupId>br.com.tutoria</groupId>
    <artifactId>tutoria-ia</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

*Por quê:* o seu projeto passa a herdar do `pom.xml` da raiz, que por sua vez herda do Spring Boot. As versões do Spring Boot, do Java e do Spring AI ficam iguais para todos.

**b) Apague a propriedade `<java.version>`** do seu `<properties>`. Ela já vem da raiz.

**c) Somente no Worker:** apague o bloco `<dependencyManagement>` com o `spring-ai-bom` e a propriedade `<spring-ai.version>`. A raiz já importa o BOM do Spring AI, então as dependências do Spring AI ficam sem versão.

### 4. Registrar o seu módulo na raiz

No `pom.xml` da **raiz**, descomente a linha do seu módulo:

```xml
<modules>
    <module>gateway</module>
</modules>
```

*Por quê:* é assim que o Maven sabe que o seu projeto faz parte do build. Esta é a única mudança permitida nos arquivos da raiz sem falar com o João.

### 5. Testar

Na **raiz** do repositório:

```bash
./mvnw package -DskipTests
```

Se aparecer **BUILD SUCCESS**, tudo certo. No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

---

## O que a Parte 1 vai entregar (e como usar)

Ninguém precisa esperar essas entregas para começar. Elas vão sendo encaixadas conforme ficam prontas, e este README será atualizado a cada entrega.

| Status | Entrega | O que muda para você |
|---|---|---|
| ✅ | Estrutura do repositório e `pom.xml` raiz | Crie seu projeto seguindo as instruções acima |
| ✅ | `docker-compose.yml` (RabbitMQ e Ollama) | `docker compose up -d` sobe tudo. Painel do RabbitMQ em `http://localhost:15672` |
| ⏳ | Módulo `contratos` | Gateway e Worker adicionam a dependência `contratos` e usam `@Import(MensageriaConfig.class)`. As filas, a serialização JSON e os DTOs vêm prontos |
| ⏳ | Segurança JWT no Gateway | Login em `POST /auth/login`. O token vai no header `Authorization: Bearer <token>`. No Gateway, o usuário logado é obtido com `@AuthenticationPrincipal` |
| ⏳ | README final e diagrama | Instruções completas de execução para a apresentação |

**Enquanto isso, cada parte pode avançar assim:**

- **Gateway (Parte 2):** endpoints `POST /perguntas` e `GET /stream` (SSE), ainda sem segurança.
- **Worker (Parte 3):** integração do Spring AI com o Ollama, testando a chamada à LLM isoladamente.
- **Front-end (Parte 4):** telas de login e de chat usando dados falsos.
