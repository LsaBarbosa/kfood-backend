create table payment_webhook_event (
    id uuid primary key,
    payment_id uuid null,
    provider_name varchar(100) not null,
    external_event_id varchar(150) not null,
    signature_valid boolean null,
    raw_payload text not null,
    processing_status varchar(30) not null default 'RECEIVED',
    received_at timestamp with time zone not null,
    processed_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_payment_webhook_event_payment
        foreign key (payment_id) references payment (id),
    constraint uk_payment_webhook_event_provider_external_event
        unique (provider_name, external_event_id),
    constraint ck_payment_webhook_event_processing_status
        check (processing_status in ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED'))
);

create index idx_payment_webhook_event_payment_id
    on payment_webhook_event (payment_id);

create index idx_payment_webhook_event_provider_status_received_at
    on payment_webhook_event (provider_name, processing_status, received_at);
