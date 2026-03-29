# Validação final Sprint 12
## Escopo validado
- Implementação funcional de pagamento da Sprint 12 no código-fonte disponível.
- Contratos de API, erros padronizados, snapshots de pagamento em pedido e persistência relacionada.
- Aderência arquitetural real dos módulos `payment`, `order` e `merchant`.
- Evidência de testes, cobertura, diff coverage e workflow de CI a partir dos artefatos existentes no repositório.

## Fontes/documentos considerados
- `README.md`
- `build.gradle`
- `buildSrc/build.gradle`
- `.github/workflows/ci.yml`
- `src/main/java/com/kfood/payment/**`
- `src/main/java/com/kfood/order/**`
- `src/main/java/com/kfood/merchant/**`
- `src/main/java/com/kfood/shared/**`
- `src/main/resources/db/migration/V18__checkout_quote_and_order_creation_support.sql`
- `src/main/resources/db/migration/V23__payment.sql`
- `src/main/resources/db/migration/V24__cash_payment_and_order_payment_method_snapshot.sql`
- `src/main/resources/db/migration/V25__payment_expires_at.sql`
- `src/test/java/com/kfood/payment/**`
- `src/test/java/com/kfood/order/**`
- `src/test/java/com/kfood/architecture/ArchitectureTest.java`
- Execução dinâmica dos comandos Gradle obrigatórios nesta validação

## Resultado executivo
A Sprint 12 tem evidência concreta de implementação funcional para domínio de pagamento, dinheiro, abstração PSP, Pix sandbox e status de pagamento, com testes unitários e WebMvc específicos. Porém a homologação final fica comprometida por dois pontos centrais ainda não resolvidos: aderência arquitetural apenas parcial nos módulos `payment`, `order` e `merchant`, e impossibilidade de homologar diff coverage/CI porque `build.gradle` referencia `com.kfood.build.DiffCoverageSupport`, mas esse suporte não existe em código-fonte versionado no repositório e hoje impede a própria compilação do build.

## Checklist por critério
- Critério:
  S12-01 Domínio de pagamento
- Evidência encontrada:
  Há módulo `payment` com domínio explícito (`PaymentMethod`, `PaymentStatus`, `PaymentStatusSnapshot`, `PaymentStatusTransitionException`), entidade `Payment`, casos de uso e integração com pedido. A criação de pagamento Pix pendente e o mapeamento para snapshot estão implementados.
- Status: OK
- Impacto:
  O escopo base do domínio de pagamento está presente e operacional em nível de código.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/domain/PaymentMethod.java`
  `src/main/java/com/kfood/payment/domain/PaymentStatus.java`
  `src/main/java/com/kfood/payment/domain/PaymentStatusSnapshot.java`
  `src/main/java/com/kfood/payment/infra/persistence/Payment.java`
  `src/main/java/com/kfood/payment/app/CreatePaymentUseCase.java`
  `src/test/java/com/kfood/payment/app/CreatePaymentUseCaseTest.java`
  `src/test/java/com/kfood/payment/infra/persistence/PaymentEntityTest.java`
- Observações:
  O domínio existe, mas a entidade principal `Payment` está em `infra.persistence`, não em `domain`.

- Critério:
  S12-02 Dinheiro
- Evidência encontrada:
  Fluxo de dinheiro implementado por `RegisterCashPaymentUseCase`, com validação de `cash_payment_enabled`, atualização de `paymentMethodSnapshot` no pedido e criação de `Payment` pendente vinculado ao pedido.
- Status: OK
- Impacto:
  O pagamento em dinheiro faz parte do fluxo real da criação de pedido e preserva snapshot do pedido.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/app/RegisterCashPaymentUseCase.java`
  `src/main/java/com/kfood/payment/app/CashPaymentNotEnabledException.java`
  `src/main/resources/db/migration/V24__cash_payment_and_order_payment_method_snapshot.sql`
  `src/test/java/com/kfood/payment/app/RegisterCashPaymentUseCaseTest.java`
  `src/main/java/com/kfood/order/app/CreatePublicOrderService.java`
- Observações:
  A evidência é forte em código e testes unitários. Não houve execução dinâmica por falha global de build.

- Critério:
  S12-03 Abstração PSP
- Evidência encontrada:
  Existe abstração por porta/registry (`PixChargeGateway`, `PixChargeGatewayRegistry`, `CreatePixChargeUseCase`), com resolução por `providerCode`, validação de resposta e encapsulamento de falhas padronizadas em `PaymentGatewayException`.
- Status: OK
- Impacto:
  O projeto suporta troca de provider sem alterar o caso de uso de pagamento Pix.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/app/gateway/PixChargeGateway.java`
  `src/main/java/com/kfood/payment/app/gateway/PixChargeGatewayRegistry.java`
  `src/main/java/com/kfood/payment/app/CreatePixChargeUseCase.java`
  `src/main/java/com/kfood/payment/app/gateway/PixChargeGatewayResponseValidator.java`
  `src/test/java/com/kfood/payment/app/CreatePixChargeUseCaseTest.java`
  `src/test/java/com/kfood/payment/app/gateway/PixChargeGatewayRegistryTest.java`
  `src/test/java/com/kfood/payment/app/gateway/PixChargeGatewayResponseValidatorTest.java`
- Observações:
  A abstração PSP está bem representada; o ponto fraco não é funcional, e sim arquitetural, porque o módulo `payment.app` ainda depende de entidades de `infra`.

- Critério:
  S12-04 Pix sandbox
- Evidência encontrada:
  Existe provider sandbox/mock real (`MockPixChargeGateway`) registrado por código `mock`, retornando `providerReference`, `qrCodePayload` e `expiresAt` determinísticos; o endpoint operacional usa esse fluxo via `CreateOrderPixPaymentUseCase`.
- Status: OK
- Impacto:
  Há implementação concreta de sandbox/mock para homologação funcional local da cobrança Pix.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/infra/gateway/MockPixChargeGateway.java`
  `src/main/java/com/kfood/payment/app/CreateOrderPixPaymentUseCase.java`
  `src/test/java/com/kfood/payment/infra/gateway/MockPixChargeGatewayTest.java`
  `src/test/java/com/kfood/payment/app/CreateOrderPixPaymentUseCaseTest.java`
  `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java`
- Observações:
  O provider implementado é `mock`; não há evidência de outro sandbox além desse adapter.

- Critério:
  S12-05 Status de pagamento
- Evidência encontrada:
  O fluxo de atualização de status permite `PENDING -> CONFIRMED|FAILED|CANCELED|EXPIRED`, preenche `confirmedAt` quando confirmado e sincroniza `paymentStatusSnapshot` do pedido via `UpdatePaymentStatusUseCase`.
- Status: OK
- Impacto:
  O status técnico do pagamento e o snapshot do pedido permanecem coerentes com o fluxo esperado da Sprint 12.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/app/UpdatePaymentStatusUseCase.java`
  `src/main/java/com/kfood/payment/infra/persistence/Payment.java`
  `src/main/java/com/kfood/order/api/UpdateOrderPaymentStatusResponse.java`
  `src/test/java/com/kfood/payment/app/UpdatePaymentStatusUseCaseTest.java`
  `src/test/java/com/kfood/order/api/OrderPaymentStatusControllerWebMvcTest.java`
- Observações:
  Não foi encontrada antecipação de webhook persistido da Sprint 13; há apenas permissão de rota em segurança e o enum `WEBHOOK_SIGNATURE_INVALID`, sem controller/handler implementado.

- Critério:
  Ausência de antecipação indevida de webhook/idempotência persistida da Sprint 13
- Evidência encontrada:
  Não foi encontrado endpoint/controller de webhook em `src/main/java`. Há permissão em segurança para `/v1/payments/webhooks/**`, mas sem implementação concreta. Já existe persistência de idempotência no projeto (`idempotency_key`) e uso real em criação de pedido público; no Pix da Sprint 12 o `Idempotency-Key` é apenas propagado ao provider, sem persistência específica de pagamento.
- Status: PARCIAL
- Impacto:
  Não há antecipação indevida de webhook implementado, mas existe infraestrutura geral de idempotência anterior no projeto; isso não comprova persistência específica de Pix e também não a introduz.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/shared/config/SecurityConfiguration.java`
  `src/main/java/com/kfood/shared/exceptions/ErrorCode.java`
  `src/main/java/com/kfood/payment/app/CreateOrderPixPaymentUseCase.java`
  `src/main/java/com/kfood/order/api/OrderController.java`
  `src/main/resources/db/migration/V18__checkout_quote_and_order_creation_support.sql`
- Observações:
  Com a evidência disponível, o melhor julgamento é parcial: não há webhook/persistência específica de Pix, mas há base genérica de idempotência já existente no produto.

- Critério:
  Contrato de API do endpoint Pix e payloads request/response
- Evidência encontrada:
  O endpoint `POST /v1/orders/{orderId}/payments/pix` existe, recebe `amount` e `provider`, aceita `Idempotency-Key` opcional e responde com `paymentId`, `orderId`, `paymentMethod`, `technicalPaymentStatus`, `paymentStatusSnapshot`, `providerReference`, `qrCodePayload` e `expiresAt`. Há testes WebMvc cobrindo header presente e ausente.
- Status: OK
- Impacto:
  O contrato principal de criação de pagamento Pix está materializado e testado em camada HTTP.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/order/api/OrderController.java`
  `src/main/java/com/kfood/order/api/CreatePixPaymentRequest.java`
  `src/main/java/com/kfood/order/api/CreatePixPaymentResponse.java`
  `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java`
- Observações:
  O response expõe simultaneamente `status` e `technicalPaymentStatus`; isso é coerente com os testes, embora haja duplicidade semântica.

- Critério:
  Erro padronizado do PSP e status HTTP de indisponibilidade do provider
- Evidência encontrada:
  `PaymentGatewayException` mapeia `PROVIDER_UNAVAILABLE` para `HttpStatus.SERVICE_UNAVAILABLE`, `TIMEOUT` para `GATEWAY_TIMEOUT`, `INVALID_REQUEST` para `BAD_GATEWAY` e `PROVIDER_NOT_SUPPORTED` para `BAD_REQUEST`. Os testes WebMvc verificam corpo padronizado com `code`, `message` e `path`.
- Status: OK
- Impacto:
  A API sinaliza falhas de provider de modo controlado e consistente com erro de integração.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/app/gateway/PaymentGatewayException.java`
  `src/main/java/com/kfood/shared/exceptions/GlobalExceptionHandler.java`
  `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java`
- Observações:
  O requisito específico de indisponibilidade do provider está atendido com `503 Service Unavailable`.

- Critério:
  Exposição de snapshots no detalhe do pedido
- Evidência encontrada:
  `OrderDetailResponse.PaymentDetail` expõe `paymentMethodSnapshot` e `paymentStatusSnapshot`, e `GetOrderDetailUseCase` popula esses campos a partir de `SalesOrder`. Há teste WebMvc cobrindo a exposição dos snapshots.
- Status: OK
- Impacto:
  O detalhe operacional do pedido traz o estado de pagamento esperado pela Sprint 12.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/order/api/OrderDetailResponse.java`
  `src/main/java/com/kfood/order/app/GetOrderDetailUseCase.java`
  `src/test/java/com/kfood/order/app/GetOrderDetailUseCaseTest.java`
  `src/test/java/com/kfood/order/api/OrderDetailControllerWebMvcTest.java`
- Observações:
  O detalhe do pedido expõe snapshots, não os dados técnicos completos do `Payment`.

- Critério:
  Modelo de dados `Payment`, enums, `providerReference`, `qrCodePayload`, `expiresAt`, `confirmedAt`
- Evidência encontrada:
  A entidade `Payment` persiste `payment_method`, `provider_name`, `provider_reference`, `status`, `amount`, `qr_code_payload`, `confirmed_at` e `expires_at`; as migrations V23/V25 criam a tabela e adicionam `expires_at`. Os testes da entidade e do repositório cobrem normalização, transitions e persistência básica.
- Status: OK
- Impacto:
  Os campos nucleares da Sprint 12 existem tanto em código quanto em schema.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/infra/persistence/Payment.java`
  `src/main/resources/db/migration/V23__payment.sql`
  `src/main/resources/db/migration/V25__payment_expires_at.sql`
  `src/test/java/com/kfood/payment/infra/persistence/PaymentEntityTest.java`
  `src/test/java/com/kfood/payment/infra/persistence/PaymentRepositoryIntegrationTest.java`
- Observações:
  A migration V23 não cria `expires_at`; isso só aparece em V25, o que é coerente com evolução incremental.

- Critério:
  Snapshots no pedido e coerência das migrations
- Evidência encontrada:
  `sales_order` possui `payment_method`, `payment_method_snapshot` e `payment_status_snapshot`; `SalesOrder` inicializa snapshots e permite atualização controlada por casos de uso de pagamento. As migrations V18 e V24 refletem essa evolução.
- Status: OK
- Impacto:
  O pedido mantém snapshot do método e do status de pagamento, permitindo leitura operacional sem consultar detalhes técnicos completos do provider.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/order/infra/persistence/SalesOrder.java`
  `src/main/resources/db/migration/V18__checkout_quote_and_order_creation_support.sql`
  `src/main/resources/db/migration/V24__cash_payment_and_order_payment_method_snapshot.sql`
  `src/main/java/com/kfood/payment/app/RegisterCashPaymentUseCase.java`
  `src/main/java/com/kfood/payment/app/UpdatePaymentStatusUseCase.java`
- Observações:
  A coerência entre código e schema é suficiente para a Sprint 12.

- Critério:
  Arquitetura por camadas: separação entre `api`, `application/app`, `domain` e `infra`
- Evidência encontrada:
  O README define como alvo `api`, `application`, `domain` e `infra`, mas a estrutura real do projeto continua majoritariamente em `app`, não `application`. Além disso, `payment.app`, `order.app` e `merchant.app` dependem diretamente de classes de `infra` e, em diversos casos, também de classes de `api`.
- Status: NÃO CONFORME
- Impacto:
  A arquitetura em camadas não está plenamente aderente ao documento-fonte; a camada de aplicação/orquestração mistura DTOs HTTP, repositórios concretos e entidades de persistência, reduzindo isolamento arquitetural e testabilidade por fronteira.
- Arquivos/locais relevantes:
  `README.md`
  `src/main/java/com/kfood/payment/app/port/PaymentPersistencePort.java`
  `src/main/java/com/kfood/payment/app/port/PaymentOrderLookupPort.java`
  `src/main/java/com/kfood/order/app/CreatePublicOrderService.java`
  `src/main/java/com/kfood/order/app/GetOrderDetailUseCase.java`
  `src/main/java/com/kfood/order/app/UpdateOrderStatusUseCase.java`
  `src/main/java/com/kfood/merchant/app/CreateStoreUseCase.java`
  `src/main/java/com/kfood/merchant/app/GetPublicStoreUseCase.java`
- Observações:
  O projeto ainda usa `app` em vez de `application` na maior parte dos módulos. A exceção visível é `merchant.application.user`, que segue melhor a direção arquitetural.

- Critério:
  Cobertura arquitetural real do teste ArchUnit/Architecture
- Evidência encontrada:
  Existe `src/test/java/com/kfood/architecture/ArchitectureTest.java`, mas a cobertura arquitetural é por recortes e não pelo desenho completo real do projeto. O teste cobre `domain` contra `api/infra`, `payment.app` contra `api` e alguns repositórios concretos, `payment.app.gateway`, `catalog.app.*` e `merchant.application.user`. Não cobre o acoplamento de `order.app` com `api`/`infra`, nem o de `merchant.app` com `api`/`infra`, que hoje existe em larga escala.
- Status: PARCIAL
- Impacto:
  O teste arquitetural atual não garante conformidade total da estrutura real; módulos relevantes ainda podem divergir sem quebra automática do guardrail arquitetural.
- Arquivos/locais relevantes:
  `src/test/java/com/kfood/architecture/ArchitectureTest.java`
  `src/main/java/com/kfood/order/app/CreatePublicOrderService.java`
  `src/main/java/com/kfood/order/app/GetOrderDetailUseCase.java`
  `src/main/java/com/kfood/merchant/app/CreateStoreUseCase.java`
  `src/main/java/com/kfood/merchant/app/GetPublicStoreMenuUseCase.java`
- Observações:
  Módulos parcialmente aderentes:
  `payment`: domínio protegido, mas `app` ainda depende de `infra.persistence.Payment` e `order.infra.persistence.SalesOrder`.
  `order`: aderência parcial; `app` depende extensivamente de `api` e `infra`.
  `merchant`: aderência parcial; `merchant.app` depende extensivamente de `api` e `infra`, enquanto `merchant.application.user` é o único recorte mais alinhado ao alvo.

- Critério:
  Validação de cobertura por diff: referência no build e existência física/auditável do suporte
- Evidência encontrada:
  `build.gradle` importa `com.kfood.build.DiffCoverageSupport` e usa `changedMainClasses(...)`, porém não existe código-fonte correspondente em `buildSrc/src/**` nem em `src/**`. `git ls-files buildSrc` mostra apenas `buildSrc/build.gradle`; portanto o suporte não está versionado como código auditável no repositório.
- Status: NÃO HOMOLOGÁVEL
- Impacto:
  Não é possível homologar a cobertura por diff com a evidência disponível; além disso, a ausência do helper impede a compilação do próprio build.
- Arquivos/locais relevantes:
  `build.gradle`
  `buildSrc/build.gradle`
  `buildSrc/`
- Observações:
  Não homologável com a evidência disponível.

- Critério:
  Comportamento esperado do suporte de diff coverage e homologação de CI
- Evidência encontrada:
  O workflow `.github/workflows/ci.yml` faz checkout com `fetch-depth: 0`, o que em tese fornece histórico suficiente para cálculo de diff. Porém a lógica efetiva de diff coverage não pôde ser auditada, porque o helper referenciado não está presente no repositório como fonte versionada, e todos os comandos Gradle falham antes da execução dos testes.
- Status: NÃO HOMOLOGÁVEL
- Impacto:
  O pipeline declara cobertura e diff coverage, mas a validação real não pode ser homologada nesta revisão.
- Arquivos/locais relevantes:
  `.github/workflows/ci.yml`
  `build.gradle`
- Observações:
  O workflow aparenta estar configurado para histórico completo, mas a evidência decisiva do cálculo/falha explícita do diff não está auditável.

- Critério:
  Testes por feature da Sprint 12 e aderência mínima à matriz
- Evidência encontrada:
  Há testes específicos por feature: `CreatePaymentUseCaseTest` e `PaymentEntityTest` para domínio, `RegisterCashPaymentUseCaseTest` para dinheiro, `CreatePixChargeUseCaseTest` e `PixChargeGatewayRegistryTest` para abstração PSP, `MockPixChargeGatewayTest` e `OrderPixPaymentControllerWebMvcTest` para Pix sandbox/endpoint, `UpdatePaymentStatusUseCaseTest` e `OrderPaymentStatusControllerWebMvcTest` para status. Há ainda `GetOrderDetailUseCaseTest` e `OrderDetailControllerWebMvcTest` para snapshots no detalhe do pedido.
- Status: OK
- Impacto:
  A evidência estática de cobertura funcional mínima por feature existe.
- Arquivos/locais relevantes:
  `src/test/java/com/kfood/payment/app/CreatePaymentUseCaseTest.java`
  `src/test/java/com/kfood/payment/app/RegisterCashPaymentUseCaseTest.java`
  `src/test/java/com/kfood/payment/app/CreatePixChargeUseCaseTest.java`
  `src/test/java/com/kfood/payment/infra/gateway/MockPixChargeGatewayTest.java`
  `src/test/java/com/kfood/payment/app/UpdatePaymentStatusUseCaseTest.java`
  `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java`
  `src/test/java/com/kfood/order/api/OrderPaymentStatusControllerWebMvcTest.java`
  `src/test/java/com/kfood/order/app/GetOrderDetailUseCaseTest.java`
- Observações:
  Esta evidência é estática; a execução dinâmica não foi homologada porque o build está quebrado.

- Critério:
  Execução dinâmica dos comandos obrigatórios de teste e cobertura
- Evidência encontrada:
  Todos os comandos obrigatórios foram tentados e falharam antes da fase de testes com o mesmo erro de compilação do build: `unable to resolve class com.kfood.build.DiffCoverageSupport` em `build.gradle` linha 1.
- Status: NÃO CONFORME
- Impacto:
  Não há homologação dinâmica de testes, cobertura total nem cobertura por diff nesta validação final.
- Arquivos/locais relevantes:
  `build.gradle`
  `build/reports/problems/problems-report.html`
- Observações:
  Evidência dinâmica coletada:
  `./gradlew --no-daemon test --tests "*Payment*"` -> falhou por ausência de `com.kfood.build.DiffCoverageSupport`.
  `./gradlew --no-daemon test --tests "*Order*"` -> falhou pelo mesmo motivo.
  `./gradlew --no-daemon test --tests "*Pix*"` -> falhou pelo mesmo motivo.
  `./gradlew --no-daemon test --tests "*Architecture*"` -> falhou pelo mesmo motivo.
  `./gradlew --no-daemon test --tests "*ArchUnit*"` -> falhou pelo mesmo motivo.
  `./gradlew --no-daemon clean test` -> falhou pelo mesmo motivo.
  `./gradlew --no-daemon jacocoTestReport jacocoTestCoverageVerification` -> falhou pelo mesmo motivo.

## Conclusão final obrigatória
Sprint 12 não homologada

Justificativa:
Há implementação funcional relevante e evidência estática de testes para o escopo da Sprint 12.
Mesmo assim, os dois pontos principais pendentes não foram resolvidos com evidência homologável.
A aderência arquitetural continua parcial nos módulos `payment`, `order` e `merchant`, com mistura entre `app`, `api` e `infra`.
O suporte de diff coverage referenciado no `build.gradle` não está presente como código-fonte versionado e auditável.
Por isso a cobertura por diff não é homologável com a evidência disponível.
Além disso, todos os comandos Gradle obrigatórios falham antes da execução dos testes.
Sem build executável, sem cobertura dinâmica e com guardrail arquitetural apenas parcial, a homologação final não se sustenta.
