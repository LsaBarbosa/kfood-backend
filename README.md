# SaaS para Pequenos Comércios

Back-end do produto SaaS para pequenos comércios com foco em canal próprio de vendas, operação de pedidos e gestão básica da loja.

## Status

Este repositório está na **Sprint 1 - Fundação do projeto**.

Escopo atual:
- estrutura inicial do repositório
- documentação mínima
- padronização básica de versionamento e edição

Próximos passos:
- bootstrap do Spring Boot 4 com Java 25 e Gradle Wrapper
- estrutura modular por domínio, aplicação e infraestrutura
- dependências base: Web, Validation, Data JPA, Security e Actuator
- endpoint técnico `/ping` ou health básico

## Contexto do produto

O produto nasce como um SaaS operacional para pequenos comércios, ajudando a vender em canal próprio, organizar pedidos e reduzir dependência de marketplaces.

## Stack alvo

- Java 25
- Spring Boot 4
- Gradle Wrapper
- PostgreSQL
- Redis
- RabbitMQ
- GitHub Actions
- AWS ECS/Fargate

## Organização esperada do projeto

A organização alvo do código seguirá a abordagem de monólito modular, com separação por domínio e camadas:

```text
com.seuprojeto.identity
  |- api | application | domain | infra
com.seuprojeto.merchant
  |- api | application | domain | infra
com.seuprojeto.catalog
  |- api | application | domain | infra
com.seuprojeto.shared
  |- security | tenancy | exceptions | config | util
```

## Como usar este repositório

### 1. Inicializar Git localmente

```bash
git init
git add .
git commit -m "chore: initialize repository base"
```

### 2. Conectar ao GitHub

```bash
git remote add origin git@github.com:SEU_USUARIO/saas-pequenos-comercios-backend.git
git branch -M main
git push -u origin main
```

## Convenções iniciais

- `main` como branch principal
- commits pequenos e objetivos
- evolução incremental por sprint
- evitar complexidade prematura
- manter aderência ao escopo do MVP

## Critério de pronto desta história

- repositório criado no GitHub
- README inicial presente
- `.gitignore` presente
- `.editorconfig` presente
- arquivos versionados

## Observação

Nesta etapa ainda **não existe aplicação Spring executável**. Isso entra na próxima história da sprint.
