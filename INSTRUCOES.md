# Instruções de setup

Guia para deixar a sua máquina pronta e criar o seu projeto. Para entender e usar a mensageria (filas e mensagens), veja o [`STEPS.md`](STEPS.md).

---

## 1. Estrutura do repositório

```
dev-web/
├── pom.xml              ← pom PAI: versões de Java, Spring Boot e Spring AI (Parte 1)
├── docker-compose.yml   ← RabbitMQ e Ollama (Parte 1)
├── contratos/           ← mensagens e configuração do RabbitMQ (Parte 1)
├── gateway/             ← Spring Boot Web (Parte 2)
├── worker/              ← Spring Boot sem Web, chama a LLM (Parte 3)
└── frontend/            ← React (Parte 4)
```

Cada projeto Java (`contratos`, `gateway`, `worker`) tem o **seu próprio `pom.xml`**, onde ficam as dependências dele. O `pom.xml` da raiz só define as versões, que todos herdam.

---

## 2. Pré-requisitos

| Ferramenta | Versão | Como conferir |
|---|---|---|
| Java (JDK) | **21** | `java -version` |
| Docker + Compose | recente | `docker compose version` |
| Node.js | 20 ou mais | `node -v` |

O Maven não precisa ser instalado: use o `./mvnw` da raiz (no Windows, `mvnw.cmd`).

---

## 3. Subir o RabbitMQ e o Ollama

Na raiz do repositório:

```bash
docker compose up -d             # sobe RabbitMQ + Ollama
docker compose up -d rabbitmq    # sobe só o RabbitMQ
docker compose down              # para tudo (os dados continuam salvos)
```

| Serviço | Endereço |
|---|---|
| RabbitMQ (usado pelo código) | `localhost:5672` |
| Painel do RabbitMQ | http://localhost:15672 |
| Ollama (usado pelo Worker) | `localhost:11434` |

**Usuário e senha do RabbitMQ:** `desweb` / `desweb`

> ⚠️ O Ollama ocupa cerca de **9 GB**. Só suba o Ollama se tiver pelo menos **10 GB livres**. Quem não for trabalhar no Worker pode subir só o RabbitMQ.

---

## 4. Criar o seu projeto (Gateway ou Worker)

**a) Gere o projeto no [start.spring.io](https://start.spring.io):**
Maven · Java 21 · Spring Boot 4.1.x · Group `br.com.tutoria` · Artifact `gateway` ou `worker`

- **Gateway:** `Spring Web` + `Spring for RabbitMQ` (não adicione `Spring Security` ainda)
- **Worker:** `Spring for RabbitMQ` + `Ollama` (não adicione `Spring Web`)

**b) Coloque o conteúdo do `.zip` dentro da sua pasta** (`gateway/pom.xml`, e não `gateway/gateway/pom.xml`). Apague do seu projeto o `mvnw`, o `mvnw.cmd`, a pasta `.mvn/` e o `.gitkeep`.

**c) No `pom.xml` do seu projeto**, troque o `<parent>` por este:

```xml
<parent>
    <groupId>br.com.tutoria</groupId>
    <artifactId>tutoria-ia</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

E apague a propriedade `<java.version>`. No Worker, apague também o bloco `<dependencyManagement>` e a propriedade `<spring-ai.version>`.

**d) No `pom.xml` da raiz**, descomente o seu módulo:

```xml
<module>gateway</module>
```

**e) Teste**, na raiz:

```bash
./mvnw package -DskipTests
```

Se aparecer **BUILD SUCCESS**, está pronto. Próximo passo: [`STEPS.md`](STEPS.md).

---

## Status da Parte 1

| | Entrega |
|---|---|
| ✅ | Estrutura do repositório e `pom.xml` raiz |
| ✅ | `docker-compose.yml` (RabbitMQ e Ollama) |
| ✅ | Módulo `contratos` (ver [`STEPS.md`](STEPS.md)) |
| ⏳ | Segurança JWT no Gateway |
| ⏳ | README final e diagrama |