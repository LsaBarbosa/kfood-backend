alter table payment_webhook_event
    drop constraint ck_payment_webhook_event_processing_status_allowed;

alter table payment_webhook_event
    add constraint ck_payment_webhook_event_processing_status_allowed
        check (processing_status in ('RECEIVED', 'PROCESSED', 'FAILED_PROCESSING'));

alter table payment_webhook_event
    drop constraint ck_payment_webhook_event_processed_at_consistency;

alter table payment_webhook_event
    add constraint ck_payment_webhook_event_processed_at_consistency
        check (
            (processing_status = 'RECEIVED' and processed_at is null)
            or (processing_status in ('PROCESSED', 'FAILED_PROCESSING') and processed_at is not null)
        );
