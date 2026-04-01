alter table payment_webhook_event
    drop constraint ck_payment_webhook_event_processing_status_allowed;

alter table payment_webhook_event
    drop constraint ck_payment_webhook_event_processed_at_consistency;

update payment_webhook_event
set processing_status = 'FAILED'
where processing_status = 'FAILED_PROCESSING';

update payment_webhook_event
set processing_status = 'IGNORED'
where processing_status = 'PROCESSED'
  and payment_id is null
  and coalesce(event_type, '') <> 'PAYMENT_CONFIRMED';

alter table payment_webhook_event
    add constraint ck_payment_webhook_event_processing_status_allowed
        check (processing_status in ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED'));

alter table payment_webhook_event
    add constraint ck_payment_webhook_event_processed_at_consistency
        check (
            (processing_status = 'RECEIVED' and processed_at is null)
            or (processing_status in ('PROCESSED', 'IGNORED', 'FAILED') and processed_at is not null)
        );
