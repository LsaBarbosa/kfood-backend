create table payment (
    id uuid primary key,
    order_id uuid not null,
    payment_method varchar(20) not null,
    provider_name varchar(80),
    provider_reference varchar(120),
    status varchar(20) not null,
    amount numeric(12, 2) not null,
    qr_code_payload varchar(4000),
    confirmed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_payment_order
        foreign key (order_id) references sales_order (id) on delete cascade,
    constraint ck_payment_amount_non_negative
        check (amount >= 0),
    constraint ck_payment_method_allowed
        check (payment_method in ('CASH', 'PIX')),
    constraint ck_payment_status_allowed
        check (status in ('PENDING', 'CONFIRMED', 'FAILED', 'CANCELED', 'EXPIRED')),
    constraint ck_payment_provider_name_not_blank
        check (provider_name is null or char_length(trim(provider_name)) > 0),
    constraint ck_payment_provider_reference_not_blank
        check (provider_reference is null or char_length(trim(provider_reference)) > 0)
);

create index ix_payment_order_status_created_at
    on payment (order_id, status, created_at desc);

create index ix_payment_provider_reference
    on payment (provider_name, provider_reference);
