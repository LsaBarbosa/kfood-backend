# K-Food Backend

Back-end do K-Food, uma plataforma SaaS para pequenos comércios com foco em canal próprio de vendas, operação de pedidos e evolução gradual para um produto multi-tenant.

## Objetivo do projeto

Construir um back-end sólido para suportar:

- onboarding da loja
- catálogo / cardápio digital
- checkout
- pedidos
- pagamentos
- eventos internos
- auditoria e observabilidade
- operação em produção

A direção arquitetural do projeto é começar com um **monólito modular**, com separação clara entre **domínio**, **aplicação** e **infraestrutura**, preservando simplicidade operacional no MVP e deixando espaço para evolução futura.

## Escopo atual

Neste momento, o projeto cobre a base inicial de execução e infraestrutura local.

### Sprint 1 — Fundação do projeto

- S1-01: repositório base
- S1-02: bootstrap do Spring Boot com Java 25 e Gradle Wrapper
- S1-03: estrutura inicial de pacotes
- S1-04: dependências base
- S1-05: endpoint técnico / health básico

### Sprint 2 — DX, qualidade e CI

- S2-01: infraestrutura local com Docker Compose para PostgreSQL, Redis e RabbitMQ

## Stack atual

### Base do projeto

- Java 25
- Spring Boot 4
- Gradle Wrapper
- GitHub

### Dependências já adicionadas

- Spring Web
- Spring Validation
- Spring Data JPA
- Spring Security
- Spring Boot Actuator

### Infra local

- PostgreSQL
- Redis
- RabbitMQ

## Arquitetura alvo

A solução segue a direção de um **monólito modular com evolução orientada a eventos**.

Organização esperada do código:

```text
com.kfood
├── shared
│   ├── config
│   ├── security
│   ├── tenancy
│   ├── exceptions
│   └── util
├── <modulo>
│   ├── api
│   ├── application
│   ├── domain
│   └── infra
```

### Separação por camada

- `api`: controllers, requests, responses e DTOs
- `application`: casos de uso e orquestração
- `domain`: regras de negócio, entidades e value objects
- `infra`: persistência, integrações, configurações e adapters

## Pré-requisitos

Antes de rodar o projeto, tenha instalado:

- JDK 25
- Docker
- Docker Compose
- Git

## Como executar a aplicação

### Subir a aplicação

```bash
./gradlew bootRun
```

### Gerar build

```bash
./gradlew build
```

### Rodar testes

```bash
./gradlew test
```

## Infraestrutura local

O projeto possui um `docker-compose.yml` para subir as dependências locais.

### Subir serviços

```bash
docker compose up -d
```

### Verificar status

```bash
docker compose ps
```

### Ver logs

```bash
docker compose logs postgres
docker compose logs redis
docker compose logs rabbitmq
```

### Derrubar serviços

```bash
docker compose down
```

### Derrubar serviços e volumes

```bash
docker compose down -v
```

## Serviços expostos localmente

| Serviço | Porta |
|---------|-------|
| PostgreSQL | 5432 |
| Redis | 6379 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ Management | 15672 |

## Credenciais locais padrão

### PostgreSQL

- database: `kfood`
- username: `kfood`
- password: `kfood`

### RabbitMQ

- username: `kfood`
- password: `kfood`

Painel do RabbitMQ:

- `http://localhost:15672`

## Health check / endpoint técnico

O projeto já possui um endpoint técnico básico para validação de saúde da aplicação.

### Actuator Health

```bash
curl http://localhost:8080/actuator/health
```

Exemplo de retorno esperado:

```json
{
  "status": "UP"
}
```

> Dependendo da evolução da sprint, pode existir também endpoint técnico adicional como `/ping`.

## Validação rápida da infra

### PostgreSQL

```bash
docker exec -it kfood-postgres pg_isready -U kfood -d kfood
```

### Redis

```bash
docker exec -it kfood-redis redis-cli ping
```

### RabbitMQ

```bash
docker exec -it kfood-rabbitmq rabbitmq-diagnostics -q ping
```

## Estrutura mínima esperada do projeto

```text
.
├── .editorconfig
├── .gitignore
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── docker-compose.yml
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   └── com/kfood
    │   └── resources
    └── test
        └── java
            └── com/kfood
```

## Qualidade e testes

O projeto foi iniciado já com preocupação de qualidade, mantendo a base pronta para evolução incremental.

Até aqui, a intenção é garantir:

- aplicação sobe localmente
- contexto Spring carrega corretamente
- dependências base não conflitam
- endpoint técnico responde com sucesso
- infra local sobe com PostgreSQL, Redis e RabbitMQ
- portas locais ficam documentadas

## Roadmap resumido

### Já concluído

- fundação do projeto
- bootstrap do Spring
- estrutura inicial
- dependências base
- health check técnico
- docker compose local

### Próximos passos

- profiles `local`, `test` e `prod`
- workflow de CI no GitHub Actions
- Spotless
- JaCoCo
- Flyway
- padrão global de erro
- auditoria base
- testes com Testcontainers

## Diretrizes técnicas do projeto

### Princípios

- começar simples sem comprometer qualidade
- separar responsabilidades por módulo e camada
- evitar complexidade operacional cedo demais
- tratar PostgreSQL como fonte transacional principal
- usar Redis para cache e dados temporários
- usar RabbitMQ para eventos internos do MVP
- evoluir com testes e automação desde cedo

### O que está fora do foco agora

- microserviços
- Kubernetes
- Kafka no início do MVP
- complexidade excessiva de infraestrutura antes da validação do fluxo principal

## Observações

Este repositório representa a base do back-end do produto. O objetivo neste estágio não é já cobrir todo o domínio do negócio, mas garantir que a aplicação:

- esteja executável
- tenha estrutura saudável
- possua dependências essenciais
- ofereça health check técnico
- tenha ambiente local padronizado para evolução das próximas sprints

## Licença

Definir.
