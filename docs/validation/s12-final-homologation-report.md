# Homologação final Sprint 12

## 1. Escopo funcional validado
- Domínio de pagamento: validado por evidência estática no módulo `payment` e pela suíte `*Payment*` verde.
- Dinheiro: validado por evidência estática dos fluxos de pagamento em dinheiro e pela suíte `*Payment*` verde.
- Abstração PSP: validada por evidência estática no recorte de gateway/PSP e pela suíte `*Payment*` verde.
- Pix sandbox: validado por evidência estática do endpoint/fluxo Pix e pela suíte `*Payment*` verde.
- Status de pagamento: validado por evidência estática dos casos de uso e persistência relacionados, além das suítes `*Payment*` e `*Order*` verdes.

## 2. Estado arquitetural
- Resultado do `ArchitectureTest`: `BUILD SUCCESSFUL`.
- Situação de `payment`: `payment.app` está coberto explicitamente pela regra que proíbe dependência em `..infra..`; não houve violação na execução.
- Situação de `order`: `order.app` está coberto explicitamente pela regra que proíbe dependência em `..infra..`; não houve violação na execução.
- Situação de `merchant`: `merchant.app` e `merchant.application.user` estão cobertos explicitamente pela regra que proíbe dependência em `..infra..`; não houve violação na execução.
- O `ArchitectureTest` atual valida a estrutura real do repositório, tratando `app` como camada de aplicação efetiva e preservando também as regras de `domain -> api/infra` e `app/application -> api`.

## 3. Build e testes
- `./gradlew --no-daemon test --tests "*Architecture*"`: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon test --tests "*Payment*"`: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon test --tests "*Order*"`: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon test --tests "*Merchant*"`: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon clean test`: `BUILD SUCCESSFUL`
- `./gradlew --no-daemon jacocoTestReport jacocoTestCoverageVerification`: `BUILD SUCCESSFUL`
- Primeira causa raiz real de qualquer falha: não houve falha nesta rodada final. As únicas ocorrências observadas foram warnings não bloqueantes da JVM/JNA durante a execução dos testes.

## 4. Cobertura e diff coverage
- Estado do JaCoCo: `BUILD SUCCESSFUL`, com geração de relatório e verificação de cobertura concluídas.
- Estado do `DiffCoverageSupport`: presente e versionado em `buildSrc/src/main/java/com/kfood/build/DiffCoverageSupport.java`.
- O `build.gradle` usa `DiffCoverageSupport.changedMainClasses(...)` para calcular classes alteradas e aplicar regra de cobertura por classe no `jacocoTestCoverageVerification`.
- O helper tenta `git diff diffBase...HEAD` e faz fallback para `git diff diffBase HEAD`; se ambos falharem, lança erro explícito (`IllegalStateException`), sem mascarar problema de cálculo de diff.
- A cobertura por classes alteradas ficou validada como executável nesta branch, e o CI segue coerente com isso porque `.github/workflows/ci.yml` usa `fetch-depth: 0`.

## 5. Conclusão final
Sprint 12 homologada

A evidência dinâmica desta rodada final ficou toda verde.
Os módulos críticos `payment`, `order` e `merchant` passaram pelo guardrail arquitetural com a estrutura real do projeto.
O build completo executou sem falhas.
O JaCoCo e a cobertura por diff executaram com sucesso usando o helper versionado e o CI compatível.
Não houve conflito entre evidência estática e dinâmica nesta validação final.
