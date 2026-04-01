create table payment_webhook_event (
    id uuid primary key,
    payment_id uuid,
    provider_name varchar(80) not null,
    external_event_id varchar(120) not null,
    event_type varchar(120),
    signature_valid boolean not null,
    raw_payload text not null,
    processing_status varchar(20) not null,
    received_at timestamp with time zone not null,
    processed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_payment_webhook_event_payment
        foreign key (payment_id) references payment (id),
    constraint uk_payment_webhook_event_provider_external_event
        unique (provider_name, external_event_id),
    constraint ck_payment_webhook_event_provider_name_not_blank
        check (char_length(trim(provider_name)) > 0),
    constraint ck_payment_webhook_event_external_event_id_not_blank
        check (char_length(trim(external_event_id)) > 0),
    constraint ck_payment_webhook_event_raw_payload_not_blank
        check (char_length(trim(raw_payload)) > 0),
    constraint ck_payment_webhook_event_event_type_not_blank
        check (event_type is null or char_length(trim(event_type)) > 0),
    constraint ck_payment_webhook_event_processing_status_allowed
        check (processing_status in ('RECEIVED', 'PROCESSED')),
    constraint ck_payment_webhook_event_processed_at_consistency
        check (
            (processing_status = 'RECEIVED' and processed_at is null)
            or (processing_status = 'PROCESSED' and processed_at is not null)
        )
);

create index ix_payment_webhook_event_status_received_at
    on payment_webhook_event (processing_status, received_at asc);

create index ix_payment_webhook_event_payment_id
    on payment_webhook_event (payment_id);
