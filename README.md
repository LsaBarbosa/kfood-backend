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

---

## Visão do produto

O produto não nasce como marketplace completo no MVP.

A proposta é entregar um **canal próprio de vendas e operação** para pequenos comércios, permitindo:

- vender pelo próprio link
- organizar melhor os pedidos
- reduzir dependência de marketplaces
- manter relacionamento com a base de clientes
- operar com menor complexidade no início

Segmentos iniciais mais aderentes:

- pizzaria
- hamburgueria
- açaí / lanches
- doceria
- marmitaria

---

## Escopo atual do projeto

Até este ponto, o projeto cobre a base estrutural necessária para evoluir com segurança, incluindo:

### Sprint 1 — Fundação do projeto

- S1-01: repositório base
- S1-02: bootstrap do Spring Boot com Java 25 e Gradle Wrapper
- S1-03: estrutura inicial de pacotes
- S1-04: dependências base
- S1-05: endpoint técnico / health básico

### Sprint 2 — DX, qualidade e CI

- S2-01: infraestrutura local com Docker Compose para PostgreSQL, Redis e RabbitMQ
- S2-02: profiles `local`, `test` e `prod`
- S2-03: pipeline de build e testes com GitHub Actions
- S2-04: formatação automática com Spotless
- S2-05: cobertura com JaCoCo

### Sprint 3 — Base técnica e banco

- S3-01: Flyway com migration baseline
- S3-02: padrão global de erro
- S3-03: auditoria base com `createdAt` e `updatedAt`
- S3-04: integração real com PostgreSQL via Testcontainers
- S3-05: health / readiness no Actuator

---

## Stack atual

### Base do projeto

- Java 25
- Spring Boot 4
- Gradle Wrapper
- GitHub
- GitHub Actions

### Dependências principais

- Spring Web
- Spring Validation
- Spring Data JPA
- Spring Security
- Spring Boot Actuator
- Flyway
- PostgreSQL Driver

### Qualidade e testes

- JUnit 5
- Mockito
- Testcontainers
- JaCoCo
- Spotless
- ArchUnit

### Infraestrutura local

- PostgreSQL
- Redis
- RabbitMQ

---

## Arquitetura alvo

A solução segue a direção de um **monólito modular com evolução orientada a eventos**.

Estrutura sugerida do código:

```text
com.kfood
├── shared
│   ├── config
│   ├── security
│   ├── exceptions
│   ├── tenancy
│   └── util
├── <module>
│   ├── api
│   ├── application
│   ├── domain
│   └── infra
```

### Separação por camada

- `api`: controllers, requests, responses e DTOs
- `application`: casos de uso e orquestração
- `domain`: regras de negócio, entidades e value objects
- `infra`: persistência, integrações, adapters e configurações técnicas

### Diretrizes arquiteturais

- começar simples sem comprometer qualidade
- priorizar coesão por módulo de negócio
- manter separação clara de responsabilidades
- usar PostgreSQL como fonte transacional principal
- usar Redis para cache e dados temporários
- usar RabbitMQ para eventos internos assíncronos
- preparar a base para multi-tenancy lógico

---

## Estrutura atual esperada do projeto

```text
.
├── .editorconfig
├── .gitattributes
├── .gitignore
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── docker-compose.yml
├── README.md
├── .github
│   └── workflows
│       └── ci.yml
└── src
    ├── main
    │   ├── java
    │   │   └── com/kfood
    │   │       ├── shared
    │   │       └── ...
    │   └── resources
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-test.yml
    │       ├── application-prod.yml
    │       └── db
    │           └── migration
    └── test
        └── java
            └── com/kfood
```

---

## Pré-requisitos

Antes de rodar o projeto, tenha instalado:

- JDK 25
- Docker
- Docker Compose
- Git

---

## Como subir a infraestrutura local

O projeto usa `docker-compose.yml` para subir dependências locais.

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

---

## Serviços expostos localmente

| Serviço | Porta |
|---------|-------|
| PostgreSQL | 5432 |
| Redis | 6379 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ Management | 15672 |

---

## Credenciais locais padrão

### PostgreSQL

- database: `kfood`
- username: `kfood`
- password: `kfood`

### RabbitMQ

- username: `kfood`
- password: `kfood`

Painel do RabbitMQ:

```text
http://localhost:15672
```

---

## Como executar a aplicação

### Rodar com profile local

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Rodar build

```bash
./gradlew build
```

### Rodar testes

```bash
./gradlew test
```

### Rodar verificação completa

```bash
./gradlew clean build
```

---

## Profiles da aplicação

O projeto já está preparado para múltiplos ambientes.

### `local`

Usado no desenvolvimento local com infraestrutura via Docker Compose.

### `test`

Usado para testes automatizados, incluindo testes de integração.

### `prod`

Usado para execução em ambiente produtivo, com dependências e segredos externalizados.

### Exemplo de ativação de profile

```bash
SPRING_PROFILES_ACTIVE=local
```

ou

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## Banco de dados e migrations

O projeto usa **Flyway** para versionamento do schema.

### Estrutura esperada das migrations

```text
src/main/resources/db/migration
```

### Convenção

```text
V1__baseline.sql
V2__...
V3__...
```

### Regras importantes

- migrations aplicadas não devem ser editadas
- alterações incompatíveis devem seguir estratégia forward-only
- toda mudança estrutural do banco deve passar por migration

---

## Health checks e endpoints técnicos

O projeto expõe endpoints técnicos via **Spring Boot Actuator**.

### Health geral

```bash
curl http://localhost:8080/actuator/health
```

Exemplo esperado:

```json
{
  "status": "UP"
}
```

### Readiness

```bash
curl http://localhost:8080/actuator/health/readiness
```

### Liveness

```bash
curl http://localhost:8080/actuator/health/liveness
```

### Observação

A Sprint 3 fecha a base de saúde da aplicação com health/readiness documentados, preparando o serviço para ambientes mais próximos de produção.

---

## Padrão de erro da API

A aplicação adota um payload padronizado para erros previsíveis e inesperados.

### Estrutura esperada

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "timestamp": "2026-03-19T10:00:00Z",
  "path": "/v1/example",
  "details": [
    {
      "field": "name",
      "message": "must not be blank"
    }
  ]
}
```

### Objetivos do padrão

- evitar retorno inconsistente de erro
- não vazar stacktrace para o consumidor
- facilitar troubleshooting
- padronizar validação e regras de negócio

---

## Public Store API

O repositório expõe uma leitura pública da loja por slug. O contrato abaixo reflete o comportamento implementado hoje, sem inventar campos sem backing real no modelo.

### GET `/v1/public/stores/{slug}`

Consulta os dados públicos da loja.

- acesso esperado: público, sem autenticação
- o payload retorna apenas campos públicos já suportados pela implementação
- o payload não expõe `id`, `cnpj`, `timezone` nem outros campos internos do tenant
- o payload não inclui mídia ou flags comerciais sem suporte persistido, como `logoUrl`, `bannerUrl` ou campos equivalentes

Response `200 OK`:

```json
{
  "slug": "loja-do-bairro",
  "name": "Loja do Bairro",
  "status": "ACTIVE",
  "phone": "21999990000",
  "hours": [
    {
      "dayOfWeek": "MONDAY",
      "openTime": "10:00:00",
      "closeTime": "22:00:00",
      "closed": false
    }
  ],
  "deliveryZones": [
    {
      "zoneName": "Centro",
      "feeAmount": 6.50,
      "minOrderAmount": 25.00
    }
  ]
}
```

Response `404 Not Found`:

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Store not found for slug: loja-inexistente"
}
```

---

## Merchant Store API

Os endpoints abaixo refletem o comportamento atual implementado para a área da loja do merchant na Sprint 6.

Contrato formal do aceite de termos: [docs/api/merchant-store-terms-acceptance-contract.md](/home/kronos/Documentos/Codigin/kfood-backend/docs/api/merchant-store-terms-acceptance-contract.md)

### POST `/v1/merchant/store/terms-acceptance`

Registra a aceitação de um documento legal para o tenant autenticado.

- acesso esperado: `OWNER`
- `acceptedAt` é gerado no servidor
- o backend deriva `requestIp` dos metadados da requisição HTTP; esse valor não faz parte do payload público
- o cliente envia apenas `documentType` e `documentVersion`
- o cliente não envia `acceptedAt` nem `requestIp` no payload
- a resposta `201 Created` inclui `acceptedAt`

Request:

```json
{
  "documentType": "TERMS_OF_USE",
  "documentVersion": "2026.03"
}
```

Response `201 Created`:

```json
{
  "id": "2d30e5c1-1d72-4ed1-a8b6-1e78a8b06f43",
  "documentType": "TERMS_OF_USE",
  "documentVersion": "2026.03",
  "acceptedAt": "2026-03-20T10:15:00Z"
}
```

### GET `/v1/merchant/store/terms-acceptance/history`

Consulta o histórico de aceite legal do tenant autenticado.

- acesso esperado: `OWNER`
- itens retornam em ordem decrescente de `acceptedAt`
- os itens refletem aceites persistidos com `acceptedAt` gerado no servidor
- os campos públicos continuam limitados ao contrato já exposto pela API

Response `200 OK`:

```json
[
  {
    "id": "2d30e5c1-1d72-4ed1-a8b6-1e78a8b06f43",
    "acceptedByUserId": "b4f3df57-74cb-4cc7-a7d6-f3af375203b1",
    "documentType": "TERMS_OF_USE",
    "documentVersion": "2026.04",
    "acceptedAt": "2026-04-20T13:15:00Z"
  },
  {
    "id": "88fd1e88-df32-4b07-b113-186d1cfb2e65",
    "acceptedByUserId": "f97c6d58-c0a8-4c85-8411-6072e94e951a",
    "documentType": "TERMS_OF_USE",
    "documentVersion": "2026.03",
    "acceptedAt": "2026-03-20T13:15:00Z"
  }
]
```

### PATCH `/v1/merchant/store/status`

Atualiza o status operacional da loja atual.

- acesso esperado: `OWNER` ou `ADMIN`

Request:

```json
{
  "targetStatus": "ACTIVE"
}
```

Response `200 OK`:

```json
{
  "id": "e6b2f3ee-c5c9-42bf-b3af-2ec9072c0caf",
  "slug": "loja-do-bairro",
  "name": "Loja do Bairro",
  "status": "ACTIVE",
  "phone": "21999990000",
  "timezone": "America/Sao_Paulo",
  "hoursConfigured": true,
  "deliveryZonesConfigured": true
}
```

---

## Auditoria base

A base técnica já prevê auditoria mínima de timestamps automáticos nas entidades.

### Campos padrão

- `createdAt`
- `updatedAt`

### Regras esperadas

- `createdAt` é preenchido na criação
- `updatedAt` é atualizado em alteração
- `createdAt` não deve mudar em updates

---

## Qualidade de código

### Formatação

O projeto usa **Spotless** para padronização automática.

#### Verificar formatação

```bash
./gradlew spotlessCheck
```

#### Corrigir formatação

```bash
./gradlew spotlessApply
```

### Cobertura

O projeto usa **JaCoCo** para gerar relatório de cobertura.

#### Gerar relatório

```bash
./gradlew test jacocoTestReport
```

Relatórios costumam ficar em:

```text
build/reports/jacoco/test/html/index.html
```

---

## Integração contínua

O projeto possui pipeline mínima com GitHub Actions.

### Objetivos da pipeline

- validar build
- rodar testes
- validar formatação
- impedir merge de código quebrado

### Fluxo esperado

- push em branch
- pull request
- execução automática do workflow
- falha do pipeline bloqueia avanço do código

---

## Testes

A base atual contempla três frentes principais:

### 1. Testes de unidade

Cobrem regras isoladas de negócio e componentes específicos.

### 2. Testes de contexto / bootstrap

Validam se a aplicação sobe corretamente com os beans principais.

### 3. Testes de integração

Com **Testcontainers**, usando PostgreSQL real em container para validar persistência e comportamento mais próximo do ambiente real.

### Comando geral

```bash
./gradlew test
```

---

## Validação rápida da infraestrutura

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

---

## Estado atual do projeto

### Já concluído até o fim da Sprint 3

- repositório base criado
- bootstrap do Spring Boot com Java 25
- estrutura inicial por camadas
- dependências base adicionadas
- endpoint técnico / health básico
- infraestrutura local com PostgreSQL, Redis e RabbitMQ
- profiles `local`, `test` e `prod`
- pipeline mínima no GitHub Actions
- formatação com Spotless
- cobertura com JaCoCo
- Flyway com migration baseline
- handler global de exceções
- auditoria base com timestamps automáticos
- integração real com PostgreSQL usando Testcontainers
- health/readiness no Actuator

---

## Próximos passos

Após a Sprint 3, a evolução natural do projeto segue para:

### Sprint 4 — Segurança

- usuários e papéis
- hash seguro de senha
- login com JWT
- proteção de rotas
- RBAC

### Sprints seguintes

- onboarding da loja
- catálogo
- checkout
- pedidos
- pagamentos
- eventos internos
- auditoria e observabilidade avançada
- produção e go-live

---

## Princípios do projeto

- simplicidade operacional no MVP
- clareza arquitetural
- evolução incremental
- testes como parte do caminho principal
- responsabilidade bem definida entre camadas
- qualidade antes de aceleração de escopo

---

## Comandos úteis

### Subir infra local

```bash
docker compose up -d
```

### Rodar app

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Rodar testes

```bash
./gradlew test
```

### Rodar build completo

```bash
./gradlew clean build
```

### Validar formatação

```bash
./gradlew spotlessCheck
```

### Aplicar formatação

```bash
./gradlew spotlessApply
```

### Gerar cobertura

```bash
./gradlew jacocoTestReport
```

---

## Observações finais

Este projeto foi estruturado para servir não apenas como MVP de produto, mas também como base técnica sustentável para crescimento.

A prioridade até aqui foi montar um backend:

- executável
- testável
- observável
- com persistência versionada
- com padrão de erro
- com base de auditoria
- com pipeline mínima
- e com ambiente local reproduzível

A partir daqui, a base está pronta para entrar nas regras reais de domínio do produto.
