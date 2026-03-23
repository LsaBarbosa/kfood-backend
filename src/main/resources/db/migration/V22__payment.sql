create table payment (
    id uuid primary key,
    order_id uuid not null,
    payment_method varchar(20) not null,
    provider_name varchar(100),
    provider_reference varchar(255),
    status varchar(20) not null,
    amount numeric(12, 2) not null,
    qr_code_payload text,
    confirmed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_payment_order
        foreign key (order_id) references sales_order (id),
    constraint ck_payment_amount_non_negative
        check (amount >= 0)
);

create index ix_payment_order_id_status
    on payment (order_id, status);

create index ix_payment_provider_name_reference
    on payment (provider_name, provider_reference);
