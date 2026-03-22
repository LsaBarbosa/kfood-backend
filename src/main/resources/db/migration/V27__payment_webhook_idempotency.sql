alter table payment_webhook_event
    add column idempotency_key varchar(200);

update payment_webhook_event
set idempotency_key = external_event_id
where idempotency_key is null;

alter table payment_webhook_event
    alter column idempotency_key set not null;

alter table payment_webhook_event
    alter column external_event_id drop not null;

alter table payment_webhook_event
    add constraint uk_payment_webhook_event_provider_idempotency_key
        unique (provider_name, idempotency_key);
