# Public Store Contract

Contrato formal do comportamento atualmente implementado para a leitura publica da loja na Sprint 6.

## GET `/v1/public/stores/{slug}`

Consulta os dados publicos minimos da loja pelo slug.

- acesso esperado: publico, sem autenticacao
- status de sucesso: `200 OK`
- retorna somente os campos publicos minimos suportados hoje pela implementacao
- nao expõe `id`, `cnpj`, `timezone`, `createdAt`, `hoursConfigured`, `deliveryZonesConfigured` ou outros campos internos
- nao expõe `logoUrl`, `bannerUrl`, `acceptsDelivery`, `acceptsPickup` ou flags sem suporte real no runtime
- `deliveryZones` nao expõe metadados internos como `id`, `active` ou `storeId`
- este contrato cobre apenas `/v1/public/stores/{slug}` e nao altera o contrato de `/v1/public/stores/{slug}/menu`

Shape oficial da resposta `200 OK`:

- top-level:
  - `slug`
  - `name`
  - `status`
  - `phone`
  - `hours`
  - `deliveryZones`
- `hours[]`:
  - `dayOfWeek`
  - `openTime`
  - `closeTime`
  - `closed`
- `deliveryZones[]`:
  - `zoneName`
  - `feeAmount`
  - `minOrderAmount`

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
