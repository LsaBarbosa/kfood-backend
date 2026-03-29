# Homologação técnica final Sprint 12

## 1. Estado funcional
- A Sprint 12 permanece funcionalmente implementada no recorte validado neste repositório.
- O fluxo de pagamentos continua cobrindo domínio de pagamento, dinheiro, abstração PSP, Pix sandbox e atualização de status.
- Os módulos centrais `payment`, `order` e `merchant` seguem com comportamento preservado após os ajustes arquiteturais residuais.
- Não houve alteração de contrato HTTP nesta etapa.

## 2. Estado arquitetural
- Resultado do `ArchitectureTest`: verde.
- O projeto continua usando majoritariamente `app` como camada de aplicação real, apesar do alvo documental seguir `api / application / domain / infra`.
- Regras validadas em [ArchitectureTest.java](/home/kronos/Documentos/Codigin/kfood-backend/src/test/java/com/kfood/architecture/ArchitectureTest.java):
  - `domain` não depende de `api` nem `infra`
  - `payment.app`, `order.app` e `merchant.app` não dependem de `infra`
  - `merchant.application.user` não depende de `infra`
  - `app` e `application` não dependem de DTOs HTTP em `api`
  - contracts de aplicação (`app.port`, `gateway`, comandos/outputs/results/queries) não dependem de `infra`
  - `payment.app.gateway`, `catalog.app.availability` e `catalog.app.selection` mantêm o guardrail contra `infra`
- Módulos validados: `payment`, `order`, `merchant`
- Acoplamentos removidos nesta rodada:
  - `merchant.app` não importa mais entidades nem repositórios de `merchant.infra` ou `catalog.infra`
  - os mapeamentos concretos de `merchant` ficaram concentrados nos adapters JPA
  - `StoreOperationalGuard` deixou de depender de entidade JPA de loja

## 3. Build e testes
- `./gradlew --no-daemon test --tests "*Architecture*"`: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon test --tests "*Payment*"`: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon test --tests "*Order*"`: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon test --tests "*Merchant*"`: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon clean test`: `BUILD SUCCESSFUL`
- Observação: houve uma falha espúria anterior em `*Order*` por execução concorrente manual de dois `gradlew test` no mesmo diretório de resultados, com `NoSuchFileException` em `build/test-results/test/binary/...`. O rerun sequencial passou; a causa raiz foi concorrência de execução, não falha de código.

## 4. Cobertura e diff coverage
- `./gradlew --no-daemon jacocoTestReport jacocoTestCoverageVerification`: `BUILD SUCCESSFUL`
- JaCoCo ficou verde com a regra global do bundle e com a regra de cobertura por diff para classes alteradas.
- `DiffCoverageSupport` está versionado em [DiffCoverageSupport.java](/home/kronos/Documentos/Codigin/kfood-backend/buildSrc/src/main/java/com/kfood/build/DiffCoverageSupport.java).
- O `build.gradle` usa o helper para calcular classes alteradas a partir de `git diff`, com fallback entre `diffBase...HEAD` e `diffBase HEAD`, e falha explicitamente se não consegue calcular o diff.
- O workflow de CI em [.github/workflows/ci.yml](/home/kronos/Documentos/Codigin/kfood-backend/.github/workflows/ci.yml) usa `fetch-depth: 0`, o que torna a validação por diff auditável no pipeline.
- Resultado percebido: cobertura por diff homologável com a evidência disponível.

## 5. Conclusão final
Sprint 12 homologada

Os guardrails arquiteturais dos módulos críticos ficaram verdes sobre a estrutura real do projeto, sem exigir rename em massa para `application`.
Os acoplamentos mais graves de `order.app` e `merchant.app` com `infra` foram removidos.
Os testes segmentados e a suíte `clean test` passaram.
O JaCoCo e a verificação de cobertura por diff passaram com a configuração efetiva de CI.
Não há pendência técnica bloqueante remanescente na evidência coletada desta etapa.
