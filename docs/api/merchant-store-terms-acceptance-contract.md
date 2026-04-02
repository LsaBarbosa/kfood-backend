# Merchant Store Terms Acceptance Contract

Contrato formal do comportamento atualmente implementado para o aceite de termos da loja do merchant na Sprint 6.

## POST `/v1/merchant/store/terms-acceptance`

Registra a aceitação de um documento legal para o tenant autenticado.

- acesso esperado: `OWNER`
- status de sucesso: `201 Created`
- campos aceitos no request: `documentType`, `documentVersion`
- `acceptedAt` nao faz parte do payload publico de entrada
- `requestIp` nao faz parte do payload publico de entrada
- a resposta publica inclui `acceptedAt`

Regras server-side:

- `acceptedAt` e gerado no servidor no momento do aceite
- o backend deriva o IP do cliente a partir dos metadados da requisicao HTTP e o normaliza internamente
- o cliente nao controla nem envia `acceptedAt` ou `requestIp`

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

## GET `/v1/merchant/store/terms-acceptance/history`

Consulta o historico de aceite legal do tenant autenticado.

- acesso esperado: `OWNER`
- itens retornam em ordem decrescente de `acceptedAt`
- cada item reflete um aceite persistido com `acceptedAt` gerado no servidor

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
