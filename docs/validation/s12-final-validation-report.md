# Validação final Sprint 12
## Escopo validado
Validação final da Sprint 12 do K-Food Backend com foco em:
- escopo funcional de pagamentos;
- contrato de API e tratamento de erros;
- modelo de dados e persistência;
- aderência arquitetural real dos módulos `payment`, `order` e `merchant`;
- cobertura por diff / `DiffCoverageSupport` / CI;
- evidência estática e dinâmica dos testes obrigatórios.

## Fontes/documentos considerados
- `README.md`
- `build.gradle`
- `.github/workflows/ci.yml`
- `.github/workflows/branch-validation.yml`
- `buildSrc/src/main/java/com/kfood/build/DiffCoverageSupport.java`
- `buildSrc/src/test/java/com/kfood/build/DiffCoverageSupportTest.java`
- `src/test/java/com/kfood/architecture/ArchitectureTest.java`
- `src/main/java/com/kfood/payment/**`
- `src/main/java/com/kfood/order/**`
- `src/main/java/com/kfood/merchant/**`
- `src/main/resources/db/migration/**`
- testes em `src/test/java/com/kfood/payment/**`, `src/test/java/com/kfood/order/**`, `src/test/java/com/kfood/merchant/**`, `src/test/java/com/kfood/shared/**`

## Resultado executivo
O escopo funcional central da Sprint 12 está materializado no código: domínio de pagamento, pagamento em dinheiro, abstração de PSP, Pix sandbox e atualização de status existem com cobertura de testes específica. O contrato HTTP do Pix e o tratamento padronizado de erro do PSP também estão presentes.

Os dois pontos críticos pendentes não estão plenamente homologados. A arquitetura real continua híbrida, com `app` dominante e `application` residual, e os módulos `order` e `merchant` ainda mantêm dependências diretas de `infra` na camada de aplicação. Além disso, embora `DiffCoverageSupport` exista, esteja versionado e pareça falhar de forma explícita, a homologação real fim a fim de cobertura por diff/CI não pôde ser comprovada com a evidência dinâmica disponível.

## Checklist por critério
- Critério:
  S12-01 Domínio de pagamento.
- Evidência encontrada:
  Há modelagem explícita de domínio e estados em `src/main/java/com/kfood/payment/domain/PaymentMethod.java`, `src/main/java/com/kfood/payment/domain/PaymentStatus.java` e `src/main/java/com/kfood/payment/domain/PaymentStatusSnapshot.java`. A entidade persistida de pagamento mantém regras de transição e dados de Pix em `src/main/java/com/kfood/payment/infra/persistence/Payment.java`. Há testes específicos em `src/test/java/com/kfood/payment/infra/persistence/PaymentEntityTest.java`, `src/test/java/com/kfood/payment/app/PaymentStatusSnapshotMapperTest.java` e `src/test/java/com/kfood/payment/app/UpdatePaymentStatusUseCaseTest.java`.
- Status: OK
- Impacto:
  O núcleo funcional de pagamento está representado de forma rastreável e testável.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/domain/*`, `src/main/java/com/kfood/payment/infra/persistence/Payment.java`, `src/test/java/com/kfood/payment/**`
- Observações:
  A validação dinâmica total do módulo foi prejudicada por falhas de execução do build, mas a evidência estática do domínio é consistente.

- Critério:
  S12-02 Dinheiro.
- Evidência encontrada:
  O fluxo existe em `src/main/java/com/kfood/payment/app/RegisterCashPaymentUseCase.java`, com atualização do snapshot do pedido e persistência do pagamento pendente. O comportamento é coberto por `src/test/java/com/kfood/payment/app/RegisterCashPaymentUseCaseTest.java`. A migração `src/main/resources/db/migration/V24__cash_payment_and_order_payment_method_snapshot.sql` introduz suporte ao método e snapshot relacionado.
- Status: OK
- Impacto:
  O fluxo de pagamento em dinheiro está implementado e conectado ao pedido.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/app/RegisterCashPaymentUseCase.java`, `src/test/java/com/kfood/payment/app/RegisterCashPaymentUseCaseTest.java`, `src/main/resources/db/migration/V24__cash_payment_and_order_payment_method_snapshot.sql`
- Observações:
  O contrato HTTP não foi alterado nesta validação; a conclusão decorre de código e testes existentes.

- Critério:
  S12-03 Abstração PSP.
- Evidência encontrada:
  A abstração está presente em `src/main/java/com/kfood/payment/app/gateway/PixChargeGateway.java`, `src/main/java/com/kfood/payment/app/gateway/PixChargeGatewayRegistry.java`, `src/main/java/com/kfood/payment/app/CreatePixChargeUseCase.java` e `src/main/java/com/kfood/payment/infra/gateway/MockPixChargeGateway.java`. Há testes específicos em `src/test/java/com/kfood/payment/app/CreatePixChargeUseCaseTest.java`, `src/test/java/com/kfood/payment/app/gateway/PixChargeGatewayResponseValidatorTest.java`, `src/test/java/com/kfood/payment/app/gateway/PaymentGatewayExceptionTest.java`, `src/test/java/com/kfood/payment/app/gateway/PixChargeGatewayRegistryTest.java` e `src/test/java/com/kfood/payment/infra/gateway/MockPixChargeGatewayTest.java`.
- Status: OK
- Impacto:
  O serviço de aplicação consome um contrato de gateway e suporta seleção de provider sem acoplamento direto ao controller.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/app/gateway/*`, `src/main/java/com/kfood/payment/app/CreatePixChargeUseCase.java`, `src/main/java/com/kfood/payment/infra/gateway/MockPixChargeGateway.java`
- Observações:
  A implementação encontrada é coerente com uma abstração de PSP em sandbox.

- Critério:
  S12-04 Pix sandbox.
- Evidência encontrada:
  O endpoint Pix é exposto em `src/main/java/com/kfood/order/api/OrderController.java`; o caso de uso está em `src/main/java/com/kfood/payment/app/CreateOrderPixPaymentUseCase.java`; o provider sandbox/mock está em `src/main/java/com/kfood/payment/infra/gateway/MockPixChargeGateway.java`. Há testes em `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java`, `src/test/java/com/kfood/payment/app/CreateOrderPixPaymentUseCaseTest.java` e `src/test/java/com/kfood/payment/infra/gateway/MockPixChargeGatewayTest.java`.
- Status: PARCIAL
- Impacto:
  O fluxo Pix sandbox existe, mas a pureza de escopo da Sprint 12 não está totalmente limpa porque o repositório já contém suporte persistido de idempotência em `shared/idempotency` e migração `V18__checkout_quote_and_order_creation_support.sql`.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/order/api/OrderController.java`, `src/main/java/com/kfood/payment/app/CreateOrderPixPaymentUseCase.java`, `src/main/java/com/kfood/payment/infra/gateway/MockPixChargeGateway.java`, `src/main/java/com/kfood/shared/idempotency/*`, `src/main/resources/db/migration/V18__checkout_quote_and_order_creation_support.sql`
- Observações:
  Não foi encontrada implementação concreta de webhook de pagamento; há apenas o matcher `/v1/payments/webhooks/**` em `src/main/java/com/kfood/shared/config/SecurityConfiguration.java`.

- Critério:
  S12-05 Status de pagamento.
- Evidência encontrada:
  A atualização de status existe em `src/main/java/com/kfood/payment/app/UpdatePaymentStatusUseCase.java`, refletindo o snapshot do pedido. O endpoint de atualização e a exposição de snapshot aparecem em `src/main/java/com/kfood/order/api/OrderController.java`. Há testes em `src/test/java/com/kfood/payment/app/UpdatePaymentStatusUseCaseTest.java` e `src/test/java/com/kfood/order/api/OrderPaymentStatusControllerWebMvcTest.java`.
- Status: OK
- Impacto:
  O pedido reflete o estado de pagamento de forma consistente no nível funcional.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/app/UpdatePaymentStatusUseCase.java`, `src/main/java/com/kfood/order/api/OrderController.java`, `src/test/java/com/kfood/payment/app/UpdatePaymentStatusUseCaseTest.java`
- Observações:
  O mapeamento para `PaymentStatusSnapshot` está centralizado em `src/main/java/com/kfood/payment/app/PaymentStatusSnapshotMapper.java`.

- Critério:
  Contrato de API do Pix, payloads e snapshots no detalhe do pedido.
- Evidência encontrada:
  O endpoint Pix está em `src/main/java/com/kfood/order/api/OrderController.java`; request/response em `src/main/java/com/kfood/order/api/CreatePixPaymentRequest.java` e `src/main/java/com/kfood/order/api/CreatePixPaymentResponse.java`; o detalhe do pedido expõe snapshots em `src/main/java/com/kfood/order/api/OrderDetailResponse.java`. Há testes WebMvc em `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java` e `src/test/java/com/kfood/order/api/OrderDetailControllerWebMvcTest.java`.
- Status: OK
- Impacto:
  O contrato HTTP relevante da Sprint 12 está materializado e testado.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/order/api/OrderController.java`, `src/main/java/com/kfood/order/api/CreatePixPaymentRequest.java`, `src/main/java/com/kfood/order/api/CreatePixPaymentResponse.java`, `src/main/java/com/kfood/order/api/OrderDetailResponse.java`
- Observações:
  O endpoint mantém `Idempotency-Key` como header opcional, propagado ao comando de Pix.

- Critério:
  Erro padronizado do PSP e status HTTP de indisponibilidade do provider.
- Evidência encontrada:
  `src/main/java/com/kfood/payment/app/gateway/PaymentGatewayException.java` mapeia `PROVIDER_UNAVAILABLE` para `503 Service Unavailable`, `TIMEOUT` para `504 Gateway Timeout`, `INVALID_REQUEST` e `UNEXPECTED_ERROR` para `502 Bad Gateway`, e `PROVIDER_NOT_SUPPORTED` para `400 Bad Request`. Há evidência dinâmica de payload padronizado em `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java` e `src/test/java/com/kfood/shared/exceptions/GlobalExceptionHandlerCoverageTest.java`.
- Status: OK
- Impacto:
  Os erros do provider têm contrato HTTP explícito e consistente.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/app/gateway/PaymentGatewayException.java`, `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java`, `src/test/java/com/kfood/shared/exceptions/GlobalExceptionHandlerCoverageTest.java`
- Observações:
  A validação cobriu indisponibilidade, timeout, provider não suportado e resposta inválida.

- Critério:
  Modelo de dados e persistência de `Payment`, enums, `providerReference`, `qrCodePayload`, `expiresAt`, `confirmedAt`, snapshots no pedido e migrations.
- Evidência encontrada:
  A entidade `Payment` contém `providerReference`, `qrCodePayload`, `confirmedAt` e `expiresAt` em `src/main/java/com/kfood/payment/infra/persistence/Payment.java`. O pedido persiste snapshots em `src/main/java/com/kfood/order/infra/persistence/SalesOrder.java`. As migrações relevantes são `src/main/resources/db/migration/V23__payment.sql`, `src/main/resources/db/migration/V24__cash_payment_and_order_payment_method_snapshot.sql` e `src/main/resources/db/migration/V25__payment_expires_at.sql`. Há testes em `src/test/java/com/kfood/payment/infra/persistence/PaymentEntityTest.java`, `src/test/java/com/kfood/payment/infra/persistence/PaymentRepositoryIntegrationTest.java` e `src/test/java/com/kfood/order/infra/persistence/SalesOrderRepositoryIntegrationTest.java`.
- Status: OK
- Impacto:
  O modelo persistido esperado para a Sprint 12 está presente e coerente com as migrações.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/infra/persistence/Payment.java`, `src/main/java/com/kfood/order/infra/persistence/SalesOrder.java`, `src/main/resources/db/migration/V23__payment.sql`, `src/main/resources/db/migration/V24__cash_payment_and_order_payment_method_snapshot.sql`, `src/main/resources/db/migration/V25__payment_expires_at.sql`
- Observações:
  A homologação dinâmica das integrações de repositório ficou limitada por falhas de ambiente com Testcontainers.

- Critério:
  Separação entre `api`, `application/app`, `domain` e `infra` segundo a estrutura real do projeto.
- Evidência encontrada:
  O `README.md` descreve separação por camadas `api`, `application`, `domain` e `infra`, mas o código real usa majoritariamente `app` nos módulos `payment`, `order` e `merchant`, com ocorrência residual de `application` em `src/main/java/com/kfood/merchant/application/user/**`. O teste arquitetural em `src/test/java/com/kfood/architecture/ArchitectureTest.java` já tenta refletir essa realidade híbrida.
- Status: PARCIAL
- Impacto:
  A arquitetura real é parcialmente aderente ao documento, mas ainda não há alinhamento completo entre padrão documental e implementação.
- Arquivos/locais relevantes:
  `README.md`, `src/test/java/com/kfood/architecture/ArchitectureTest.java`, `src/main/java/com/kfood/payment/app/**`, `src/main/java/com/kfood/order/app/**`, `src/main/java/com/kfood/merchant/app/**`, `src/main/java/com/kfood/merchant/application/user/**`
- Observações:
  A presença de `app` em vez de `application` é dominante e deve ser declarada como estrutura real atual.

- Critério:
  Ausência de dependência `domain -> api/infra` e de `app/application -> api` nos módulos centrais.
- Evidência encontrada:
  Não foram encontradas importações `domain -> api/infra` em `src/main/java/com/kfood/payment/domain`, `src/main/java/com/kfood/order/domain` e `src/main/java/com/kfood/merchant/domain`. Também não foram encontradas importações `app/application -> api` em `src/main/java/com/kfood/payment/app`, `src/main/java/com/kfood/order/app`, `src/main/java/com/kfood/merchant/app` e `src/main/java/com/kfood/merchant/application`.
- Status: OK
- Impacto:
  Os acoplamentos mais graves entre domínio/aplicação e a borda HTTP não aparecem na evidência estática inspecionada.
- Arquivos/locais relevantes:
  `src/main/java/com/kfood/payment/domain/**`, `src/main/java/com/kfood/order/domain/**`, `src/main/java/com/kfood/merchant/domain/**`, `src/main/java/com/kfood/payment/app/**`, `src/main/java/com/kfood/order/app/**`, `src/main/java/com/kfood/merchant/app/**`, `src/main/java/com/kfood/merchant/application/**`
- Observações:
  Esta conclusão é estática; a suíte arquitetural ainda falha por outros acoplamentos.

- Critério:
  Ausência de dependência `app/application -> infra` e cobertura arquitetural real via ArchUnit para `payment`, `order` e `merchant`.
- Evidência encontrada:
  Há dependências diretas de `infra` em `order.app` e `merchant.app`, por exemplo: `src/main/java/com/kfood/order/app/CreatePublicOrderService.java`, `src/main/java/com/kfood/order/app/GetOrderDetailUseCase.java`, `src/main/java/com/kfood/order/app/UpdateOrderStatusUseCase.java`, `src/main/java/com/kfood/merchant/app/CreateDeliveryZoneUseCase.java`, `src/main/java/com/kfood/merchant/app/GetPublicStoreUseCase.java`, `src/main/java/com/kfood/merchant/app/GetPublicStoreMenuUseCase.java`, `src/main/java/com/kfood/merchant/app/UpdateStoreHoursUseCase.java`. Em contraste, `payment.app` está mais aderente e o grep não encontrou `payment.app -> infra`. O `ArchitectureTest` cobre a estrutura real de forma ampla para `domain` e `app/application -> api`, mas a cobertura de `app/application -> infra` ainda é parcial por recortes; a execução de `./gradlew --no-daemon jacocoTestReport jacocoTestCoverageVerification` mostrou falhas explícitas do `ArchitectureTest`, inclusive em `domainShouldNotDependOnApiOrInfra`, `applicationLayerShouldNotDependOnHttpApiDtos`, `applicationContractsShouldNotDependOnInfraImplementations`, `paymentApplicationShouldNotDependOnInfraImplementations`, `merchantUserApplicationShouldNotDependOnInfraImplementations`, `paymentGatewayApplicationShouldNotDependOnApiOrInfra`, `catalogAvailabilityShouldNotDependOnInfra` e `catalogSelectionShouldNotDependOnInfra`.
- Status: NÃO CONFORME
- Impacto:
  A aderência arquitetural completa, que era um dos pendentes principais, não está homologada. `payment` está mais próximo do padrão, mas `order` e `merchant` seguem parcialmente fora da separação esperada e o teste arquitetural não está verde.
- Arquivos/locais relevantes:
  `src/test/java/com/kfood/architecture/ArchitectureTest.java`, `src/main/java/com/kfood/order/app/CreatePublicOrderService.java`, `src/main/java/com/kfood/order/app/GetOrderDetailUseCase.java`, `src/main/java/com/kfood/order/app/UpdateOrderStatusUseCase.java`, `src/main/java/com/kfood/merchant/app/CreateDeliveryZoneUseCase.java`, `src/main/java/com/kfood/merchant/app/GetPublicStoreUseCase.java`, `src/main/java/com/kfood/merchant/app/GetPublicStoreMenuUseCase.java`, `src/main/java/com/kfood/merchant/app/UpdateStoreHoursUseCase.java`
- Observações:
  A cobertura arquitetural existente é melhor que a versão documental idealizada, mas ainda não valida de forma satisfatória uma arquitetura totalmente aderente dos módulos centrais.

- Critério:
  Presença física, versionamento, auditabilidade e comportamento fail-closed de `DiffCoverageSupport` ou equivalente.
- Evidência encontrada:
  `build.gradle` referencia `com.kfood.build.DiffCoverageSupport`. A classe existe em `buildSrc/src/main/java/com/kfood/build/DiffCoverageSupport.java`, está versionada no repositório e possui testes em `buildSrc/src/test/java/com/kfood/build/DiffCoverageSupportTest.java`. A implementação tenta `git diff` em dois formatos e lança `IllegalStateException` explícita se não consegue calcular o diff.
- Status: OK
- Impacto:
  O mecanismo de suporte a diff coverage está presente, auditável e com intenção clara de falha explícita.
- Arquivos/locais relevantes:
  `build.gradle`, `buildSrc/src/main/java/com/kfood/build/DiffCoverageSupport.java`, `buildSrc/src/test/java/com/kfood/build/DiffCoverageSupportTest.java`
- Observações:
  A existência do helper não equivale, por si só, à homologação fim a fim do fluxo de cobertura em CI.

- Critério:
  Homologação real da cobertura por diff / CI.
- Evidência encontrada:
  O workflow ` .github/workflows/ci.yml` usa `actions/checkout@v6` com `fetch-depth: 0`, o que é compatível com histórico suficiente para cálculo de diff. Porém a evidência dinâmica disponível não comprova a passagem do pipeline completo neste ambiente. `./gradlew --no-daemon test --tests "*Architecture*"` falhou por cobertura Jacoco de diff com várias classes alteradas em `merchant`, `order` e `payment` com 0.00. `./gradlew --no-daemon jacocoTestReport jacocoTestCoverageVerification` também falhou, tanto por cobertura insuficiente quanto por falhas de testes. Não há evidência local de execução verde fim a fim em condições equivalentes à CI.
- Status: NÃO HOMOLOGÁVEL
- Impacto:
  O segundo pendente principal permanece sem prova conclusiva de homologação real.
- Arquivos/locais relevantes:
  `.github/workflows/ci.yml`, `build.gradle`, `buildSrc/src/main/java/com/kfood/build/DiffCoverageSupport.java`
- Observações:
  Não homologável com a evidência disponível.

- Critério:
  Existência de testes por feature da Sprint 12 e aderência mínima à matriz de testes.
- Evidência encontrada:
  Há testes específicos para Pix, PSP, erros padronizados, snapshots, cash e status de pagamento em `src/test/java/com/kfood/payment/**`, `src/test/java/com/kfood/order/api/OrderPixPaymentControllerWebMvcTest.java`, `src/test/java/com/kfood/order/api/OrderPaymentStatusControllerWebMvcTest.java`, `src/test/java/com/kfood/order/api/OrderDetailControllerWebMvcTest.java` e `src/test/java/com/kfood/shared/exceptions/GlobalExceptionHandlerCoverageTest.java`.
- Status: OK
- Impacto:
  A Sprint 12 não está apoiada apenas em evidência estática; existe uma base relevante de testes voltados às features.
- Arquivos/locais relevantes:
  `src/test/java/com/kfood/payment/**`, `src/test/java/com/kfood/order/api/**`, `src/test/java/com/kfood/shared/exceptions/GlobalExceptionHandlerCoverageTest.java`
- Observações:
  A presença de testes não implicou sucesso do build no ambiente atual.

- Critério:
  Resultado dos comandos obrigatórios e natureza da evidência dinâmica.
- Evidência encontrada:
  `./gradlew --no-daemon test --tests "*Payment*"` falhou com `DiscoveryIssueException`, `NoSuchFileException` em `build/test-results/...in-progress-results...bin` e erro do agente Jacoco ao gravar `build/jacoco/test.exec`.
  `./gradlew --no-daemon test --tests "*Order*"` falhou com múltiplos `NoClassDefFoundError`/`ClassNotFoundException` em testes unitários/WebMvc e também com falhas de Testcontainers/Docker (`DockerClientProviderStrategy`) nas integrações.
  `./gradlew --no-daemon test --tests "*Pix*"` falhou com `IllegalStateException` em contexto Spring, `NoClassDefFoundError` em testes de uso/gateway e erros de saída Jacoco/`NoSuchFileException`.
  `./gradlew --no-daemon test --tests "*Architecture*"` falhou porque o task `test` finaliza em `jacocoTestCoverageVerification`; o filtro executado não cobre as classes alteradas e a verificação reportou bundle 0.00/0.99 e várias classes alteradas com 0.00/1.00.
  `./gradlew --no-daemon test --tests "*ArchUnit*"` falhou com `No tests found for given includes`.
  `./gradlew --no-daemon clean test` falhou com 791 testes executados e 39 falhas; a evidência dominante é indisponibilidade de Docker/Testcontainers (`DockerClientProviderStrategy`) em integrações, seguida de encerramento com `NoSuchFileException` em `build/test-results/test/binary/...`.
  `./gradlew --no-daemon jacocoTestReport jacocoTestCoverageVerification` falhou por violações do `ArchitectureTest`, cobertura por diff insuficiente e falhas adicionais de testes/integrations.
- Status: NÃO CONFORME
- Impacto:
  A evidência dinâmica final não sustenta homologação limpa da Sprint 12 neste ambiente.
- Arquivos/locais relevantes:
  `build.gradle`, `src/test/java/com/kfood/architecture/ArchitectureTest.java`, `src/test/java/com/kfood/order/**`, `src/test/java/com/kfood/payment/**`, `src/test/java/com/kfood/shared/**`
- Observações:
  Parte das falhas é ambiental, mas há falhas funcionais/estruturais reais no estado atual do repositório, inclusive no teste arquitetural.

## Conclusão final obrigatória
Sprint 12 não homologada

O escopo funcional principal de pagamentos está presente, com contrato HTTP, persistência e testes específicos. Porém os dois pendentes centrais desta validação não foram fechados: a aderência arquitetural completa não foi atingida, porque `order` e `merchant` ainda dependem diretamente de `infra` na camada `app`, e o `ArchitectureTest` falha em execução real. Além disso, embora `DiffCoverageSupport` exista e seja auditável, a homologação real fim a fim de diff coverage/CI não pôde ser comprovada com a evidência dinâmica disponível. O build completo e os comandos obrigatórios falharam, com mistura de problemas estruturais e limitações ambientais relacionadas a Testcontainers/Docker.
