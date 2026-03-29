# Diagnóstico aprofundado Sprint 12

## 1. Identificação do ambiente
- branch atual: `feat/S12/fix`
- commit atual: `7b3f4b3af34548de02e7e572f89574e91345e33b`
- java version: execução local em `OpenJDK 21.0.9`; Gradle toolchain configurada para Java 25 e `./gradlew --no-daemon --version` reportou launcher JVM `25.0.2`
- gradle version: `9.3.1`
- sistema operacional: Debian GNU/Linux 13 (`trixie`), kernel `6.12.63+deb13-amd64`
- docker disponível? não
- observações:
  - o client Docker existe, mas o daemon não ficou acessível por permissão no socket (`permission denied while trying to connect to the docker API at unix:///var/run/docker.sock`)
  - artefato bruto: [env.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/env.txt)

## 2. Estrutura real do projeto
- árvore resumida dos pacotes:
  - `src/main/java/com/kfood/payment`: `app`, `app/gateway`, `app/port`, `domain`, `infra`, `infra/gateway`, `infra/persistence`
  - `src/main/java/com/kfood/order`: `api`, `app`, `app/port`, `domain`, `infra`, `infra/adapter`, `infra/eventing`, `infra/numbering`, `infra/persistence`
  - `src/main/java/com/kfood/merchant`: `api`, `app`, `app/port`, `application`, `application/user`, `domain`, `infra`, `infra/adapter`, `infra/persistence`, `infra/user`
  - `src/main/java/com/kfood/shared`: `config`, `exceptions`, `idempotency`, `infra/persistence`, `security`, `tenancy`, `web`
- presença de:
  - `build.gradle`: sim
  - `.github/workflows/ci.yml`: sim
  - `.github/workflows/branch-validation.yml`: não encontrado
  - `buildSrc`: sim
  - `src/test/java/com/kfood/architecture/ArchitectureTest.java`: sim
- observações sobre `app` vs `application`:
  - a estrutura real é híbrida
  - `payment` e `order` usam `app` como camada de aplicação dominante
  - `merchant` usa majoritariamente `app`, mas ainda contém `merchant.application.user`
  - o README continua descrevendo `application` como arquitetura alvo
- artefato bruto: [tree-payment-order-merchant.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/tree-payment-order-merchant.txt)

## 3. Evidência funcional da Sprint 12

### 3.1 S12-01 Domínio de pagamento
- Evidência encontrada:
  - entidade `Payment` com `PaymentMethod`, `PaymentStatus`, `providerReference`, `qrCodePayload`, `confirmedAt` e `expiresAt`
  - transições de status encapsuladas em `Payment.changeStatus(...)`
  - mapeamento de snapshot do pedido em `PaymentStatusSnapshotMapper`
- Arquivos principais:
  - [Payment.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/infra/persistence/Payment.java)
  - [PaymentMethod.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/domain/PaymentMethod.java)
  - [PaymentStatus.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/domain/PaymentStatus.java)
  - [PaymentStatusSnapshot.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/domain/PaymentStatusSnapshot.java)
- Testes encontrados:
  - `src/test/java/com/kfood/payment/infra/persistence/PaymentTest.java`
  - `src/test/java/com/kfood/payment/app/UpdatePaymentStatusUseCaseTest.java`
- Status: OK

### 3.2 S12-02 Dinheiro
- Evidência encontrada:
  - `RegisterCashPaymentUseCase` registra pagamento pendente em dinheiro e atualiza snapshots do pedido
  - `CreatePublicOrderService` aciona fluxo de dinheiro quando `paymentMethod == CASH`
- Arquivos principais:
  - [RegisterCashPaymentUseCase.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/app/RegisterCashPaymentUseCase.java)
  - [CreatePublicOrderService.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/order/app/CreatePublicOrderService.java)
  - [V24__cash_payment_and_order_payment_method_snapshot.sql](/home/kronos/Documentos/Codigin/kfood-backend/src/main/resources/db/migration/V24__cash_payment_and_order_payment_method_snapshot.sql)
- Testes encontrados:
  - `src/test/java/com/kfood/payment/app/RegisterCashPaymentUseCaseTest.java`
  - `src/test/java/com/kfood/order/app/CreatePublicOrderServiceTest.java`
- Status: OK

### 3.3 S12-03 Abstração PSP
- Evidência encontrada:
  - abstração por gateway com `PixChargeGateway`, `PixChargeGatewayRegistry`, `CreatePixChargeUseCase`
  - erro padronizado em `PaymentGatewayException`
- Arquivos principais:
  - [PixChargeGateway.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/app/gateway/PixChargeGateway.java)
  - [PixChargeGatewayRegistry.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/app/gateway/PixChargeGatewayRegistry.java)
  - [PaymentGatewayException.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/app/gateway/PaymentGatewayException.java)
- Testes encontrados:
  - `src/test/java/com/kfood/payment/app/CreatePixChargeUseCaseTest.java`
  - `src/test/java/com/kfood/payment/app/gateway/PaymentGatewayExceptionTest.java`
  - `src/test/java/com/kfood/payment/app/gateway/PixChargeGatewayResponseValidatorTest.java`
- Status: OK

### 3.4 S12-04 Pix sandbox
- Evidência encontrada:
  - provider mock implementado em `MockPixChargeGateway`
  - endpoint operacional `POST /v1/orders/{orderId}/payments/pix`
  - resposta inclui `paymentId`, `providerReference`, `qrCodePayload`, `expiresAt`
- Arquivos principais:
  - [MockPixChargeGateway.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/infra/gateway/MockPixChargeGateway.java)
  - [CreateOrderPixPaymentUseCase.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/app/CreateOrderPixPaymentUseCase.java)
  - [OrderController.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/order/api/OrderController.java)
- Testes encontrados:
  - `src/test/java/com/kfood/payment/app/CreateOrderPixPaymentUseCaseTest.java`
  - `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java`
- Status: OK

### 3.5 S12-05 Status de pagamento
- Evidência encontrada:
  - caso de uso `UpdatePaymentStatusUseCase`
  - endpoint `PATCH /v1/orders/payments/{paymentId}/status`
  - atualização do snapshot do pedido para `PAID`/`FAILED`
- Arquivos principais:
  - [UpdatePaymentStatusUseCase.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/payment/app/UpdatePaymentStatusUseCase.java)
  - [OrderController.java](/home/kronos/Documentos/Codigin/kfood-backend/src/main/java/com/kfood/order/api/OrderController.java)
- Testes encontrados:
  - `src/test/java/com/kfood/payment/app/UpdatePaymentStatusUseCaseTest.java`
  - `src/test/java/com/kfood/order/api/OrderPaymentStatusControllerWebMvcTest.java`
- Status: OK

## 4. Build e cobertura
- resultado de `./gradlew --no-daemon clean test`:
  - `BUILD SUCCESSFUL`
  - artefato bruto: [gradle-clean-test.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/gradle-clean-test.txt)
- resultado de `./gradlew --no-daemon jacocoTestReport jacocoTestCoverageVerification`:
  - `BUILD FAILED`
  - artefato bruto: [gradle-jacoco.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/gradle-jacoco.txt)
- se falhar:
  - primeira causa raiz real:
    - `jacocoTestCoverageVerification` falhou por regra de cobertura do bundle e de classes alteradas
  - classes/testes mais afetados:
    - `com.kfood.merchant.infra.adapter.JpaMerchantCommandAdapter`
    - `com.kfood.order.infra.adapter.JpaOrderWorkflowAdapter`
    - `com.kfood.order.infra.adapter.JpaOrderQueryAdapter`
    - bundle `kfood-backend` abaixo de `0.99`
  - se a falha é de código, arquitetura, cobertura ou ambiente:
    - cobertura/diff coverage
- informar se o build compila:
  - sim
- informar se a cobertura executa:
  - sim, mas a verificação falha
- informar se JaCoCo gera relatório:
  - sim, `jacocoTestReport` foi gerado mesmo com a falha da verificação

## 5. ArchitectureTest e aderência arquitetural
- resultado de `./gradlew --no-daemon test --tests "*Architecture*"`:
  - `BUILD SUCCESSFUL`
  - artefato bruto: [gradle-architecture.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/gradle-architecture.txt)
- listar violações encontradas:
  - o `ArchitectureTest` atual não encontrou violação no recorte que ele cobre
  - porém o scan de imports mostra dependências restantes de `order.app -> infra` e `merchant.app -> infra` fora do recorte de `UseCase`
  - exemplos:
    - `order.app.AssignOrderNumberService -> order.infra.persistence.SalesOrder`
    - `order.app.CreatePublicOrderService -> ...Repository` e entidades `infra`
    - `merchant.app.*Mapper` e `StoreOperationalGuard` dependem de classes `infra`
- identificar módulos afetados:
  - `payment`: sem imports `app -> infra` nem `domain -> infra/api` encontrados neste scan
  - `order`: há `app -> infra`
  - `merchant`: há `app -> infra`
- explicar se:
  - `domain -> infra/api` existe:
    - não foram encontrados imports `domain -> infra/api` em `payment`, `order` e `merchant`
  - `app/application -> api` existe:
    - não foram encontrados imports `app/application -> api` nesses módulos
  - `app/application -> infra` existe:
    - sim, ainda existe em `order.app` e `merchant.app`
- classificar:
  - PARCIAL
- observação:
  - o teste arquitetural passou, mas não cobre integralmente todos os imports residuais da camada `app`
- artefato bruto: [architecture-imports.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/architecture-imports.txt)

## 6. Diff coverage / CI
- confirmar se `DiffCoverageSupport` existe fisicamente no repositório:
  - sim
- localizar arquivo-fonte exato:
  - [DiffCoverageSupport.java](/home/kronos/Documentos/Codigin/kfood-backend/buildSrc/src/main/java/com/kfood/build/DiffCoverageSupport.java)
- confirmar se `build.gradle` usa esse helper:
  - sim, em `changedMainClasses(...)`
- resumir comportamento aparente do helper:
  - como calcula diff:
    - tenta `git diff --name-only --diff-filter=ACMR <base>...HEAD -- src/main/java`
    - se falhar, faz fallback para `git diff --name-only --diff-filter=ACMR <base> HEAD -- src/main/java`
  - se falha explicitamente ou silenciosamente:
    - falha explicitamente com `IllegalStateException` quando as duas tentativas falham
  - também filtra interfaces e annotation interfaces para não exigir cobertura de tipos não executáveis
- resumir `ci.yml`:
  - checkout:
    - usa `actions/checkout@v6`
  - fetch-depth:
    - `0`
  - steps de test/cobertura:
    - `spotlessCheck`
    - `clean test jacocoTestReport jacocoTestCoverageVerification`
    - upload de relatórios JaCoCo
- observações:
  - `.github/workflows/branch-validation.yml` não existe; só foi encontrado `allow-main-only-from-development.yml`
  - a evidência local mostra helper testado e workflow com histórico suficiente, mas a execução atual do `jacocoTestCoverageVerification` falhou
- classificar:
  - PARCIAL
- artefatos brutos:
  - [ci-workflow-summary.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/ci-workflow-summary.txt)
  - [diff-coverage-summary.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/diff-coverage-summary.txt)

## 7. Execução real da aplicação
- a aplicação sobe? não
- perfil usado: `test`
- porta usada: tentativa em `18080`
- stacktrace/causa se falhar:
  - `Cannot load driver class: org.h2.Driver`
  - a falha ocorreu no `bootRun` durante criação do `DataSource`
- health endpoint responde? não
- observações:
  - o perfil `test` aponta para H2 em memória, mas o `build.gradle` declara H2 apenas em `testRuntimeOnly`
  - como a aplicação não subiu, não foi possível validar `actuator/health` nem endpoints reais
- artefatos brutos:
  - [app-startup.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/app-startup.txt)
  - [health-check.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/health-check.txt)

## 8. Exercício real dos endpoints da Sprint 12
Como a aplicação não subiu, esta seção não foi validável dinamicamente.

### 8.1 Criar pedido com CASH
- request usada:
  - não executada dinamicamente
  - evidência estática: `POST /v1/public/stores/{slug}/orders` com `paymentMethod`
- response recebida:
  - não validável com a evidência coletada
- status HTTP:
  - não validável com a evidência coletada
- campos relevantes retornados:
  - evidência estática em teste mostra `orderNumber`, `status`, `paymentStatusSnapshot`
- evidência de `paymentMethodSnapshot`:
  - estática em `RegisterCashPaymentUseCaseTest`
- evidência de `paymentStatusSnapshot`:
  - estática em `CreatePublicOrderOutput`/testes
- artefato bruto: [cash-flow.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/cash-flow.txt)

### 8.2 Criar cobrança Pix
- request usada:
  - não executada dinamicamente
  - evidência estática em teste: `POST /v1/orders/{orderId}/payments/pix` com body `{ "amount": 57.50, "provider": "mock" }`
- response recebida:
  - não validável com a evidência coletada
- status HTTP:
  - não validável com a evidência coletada
- campos relevantes retornados:
  - evidência estática em `OrderPixPaymentControllerWebMvcTest`:
    - `paymentId`
    - `orderId`
    - `paymentMethod`
    - `paymentStatusSnapshot`
    - `technicalPaymentStatus`
    - `providerReference`
    - `qrCodePayload`
    - `expiresAt`
- artefato bruto: [pix-flow.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/pix-flow.txt)

### 8.3 Atualizar status do pagamento
- request usada:
  - não executada dinamicamente
  - evidência estática em teste: `PATCH /v1/orders/payments/{paymentId}/status` com body `{ "newStatus": "CONFIRMED" }`
- response recebida:
  - não validável com a evidência coletada
- status HTTP:
  - não validável com a evidência coletada
- verificar se:
  - `confirmedAt` é preenchido ao confirmar:
    - sim, por evidência estática em `UpdatePaymentStatusUseCaseTest`
  - snapshot do pedido muda corretamente:
    - sim, por evidência estática para `PAID` e `FAILED`
- artefato bruto: [payment-status-update.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/payment-status-update.txt)

### 8.4 Erro padronizado do PSP
- status HTTP:
  - não validável dinamicamente
  - evidência estática em `OrderPixPaymentControllerWebMvcTest`:
    - `503` para provider unavailable
    - `502` para invalid response
    - `400` para provider not supported
- code:
  - evidência estática:
    - `PAYMENT_PROVIDER_UNAVAILABLE`
    - `PAYMENT_PROVIDER_INVALID_RESPONSE`
    - `PAYMENT_PROVIDER_NOT_SUPPORTED`
- message:
  - evidência estática presente nos testes WebMvc
- path:
  - evidência estática mostra `/v1/orders/{orderId}/payments/pix`
- details:
  - não validável dinamicamente com a evidência coletada
- traceId se existir:
  - não validável com a evidência coletada
- artefato bruto: [error-flow.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/error-flow.txt)

## 9. Persistência e migrations
- listar migrations de pagamento e pedido relevantes:
  - `V15__sales_order.sql`
  - `V16__sales_order_items.sql`
  - `V18__checkout_quote_and_order_creation_support.sql`
  - `V20__sales_order_status_history.sql`
  - `V21__sales_order_address_snapshot.sql`
  - `V23__payment.sql`
  - `V24__cash_payment_and_order_payment_method_snapshot.sql`
  - `V25__payment_expires_at.sql`
- confirmar presença de:
  - tabela `payment`: sim
  - `provider_reference`: sim
  - `qr_code_payload`: sim
  - `confirmed_at`: sim
  - `expires_at`: sim
  - snapshots no pedido: sim, `payment_method_snapshot` e `payment_status_snapshot`
- se possível, validar via logs de startup/migration:
  - não validável dinamicamente porque a aplicação não subiu até Flyway concluir
- classificar consistência schema vs código:
  - consistente por evidência estática entre migrations e entidades/casos de uso
- artefato bruto: [persistence-summary.txt](/home/kronos/Documentos/Codigin/kfood-backend/docs/validation/artifacts/persistence-summary.txt)

## 10. Resultado executivo
- Build: OK
- Testes: OK
- Cobertura: PARCIAL
- Arquitetura: PARCIAL
- App em execução: NÃO
- Fluxo Pix real: NÃO VALIDÁVEL
- Conclusão final: NÃO HOMOLOGADA

## 11. Principais bloqueios
1. `bootRun` falha no perfil `test` por ausência do driver `org.h2.Driver`, impedindo validação dinâmica da API.
2. `jacocoTestCoverageVerification` falha localmente por regras de diff coverage/classes alteradas, apesar de `clean test` passar.
3. O `ArchitectureTest` passa, mas ainda existem imports `order.app -> infra` e `merchant.app -> infra` fora do recorte coberto.
4. O daemon Docker não está acessível no ambiente coletado, o que limita validações que dependam de infraestrutura conteinerizada.
5. `.github/workflows/branch-validation.yml` não existe, embora tenha sido solicitado como evidência esperada.
6. O ambiente efetivo mostra divergência entre Java local principal (`21.0.9`) e toolchain/launcher do build (`25.0.2`).
7. `health` e fluxos Pix/CASH reais não puderam ser exercitados, então a análise funcional dinâmica ficou incompleta.

## 12. Próximas ações recomendadas
1. Corrigir a execução local do `bootRun` garantindo driver compatível para o perfil usado na API local.
2. Fechar as violações restantes do diff coverage nas classes `JpaMerchantCommandAdapter`, `JpaOrderWorkflowAdapter` e `JpaOrderQueryAdapter`.
3. Ampliar o recorte do `ArchitectureTest` para capturar os imports residuais de `app -> infra` além de `UseCase`.
4. Padronizar a estratégia de execução local documentando um perfil realmente executável para diagnóstico manual da API.
5. Revisar a documentação/workflows para alinhar o que é esperado (`branch-validation.yml`) com o que realmente existe no repositório.
