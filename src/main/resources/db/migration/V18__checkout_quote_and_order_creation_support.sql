alter table sales_order
    add column if not exists payment_method varchar(20) not null default 'PIX';

alter table sales_order
    add column if not exists payment_status_snapshot varchar(20) not null default 'PENDING';

create table if not exists checkout_quote (
    id uuid primary key,
    store_id uuid not null,
    customer_id uuid not null,
    fulfillment_type varchar(20) not null,
    address_id uuid null,
    subtotal_amount numeric(12, 2) not null,
    delivery_fee_amount numeric(12, 2) not null,
    total_amount numeric(12, 2) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table if not exists checkout_quote_item (
    id uuid primary key,
    quote_id uuid not null,
    product_id uuid not null,
    product_name_snapshot varchar(255) not null,
    unit_price_snapshot numeric(12, 2) not null,
    quantity integer not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_checkout_quote_item_quote
        foreign key (quote_id) references checkout_quote (id) on delete cascade
);

create table if not exists checkout_quote_item_option (
    id uuid primary key,
    quote_item_id uuid not null,
    option_name_snapshot varchar(255) not null,
    extra_price_snapshot numeric(12, 2) not null,
    quantity integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_checkout_quote_item_option_quote_item
        foreign key (quote_item_id) references checkout_quote_item (id) on delete cascade
);

create index if not exists ix_checkout_quote_store_id_expires_at
    on checkout_quote (store_id, expires_at);

create index if not exists ix_checkout_quote_item_quote_id
    on checkout_quote_item (quote_id);

create index if not exists ix_checkout_quote_item_option_quote_item_id
    on checkout_quote_item_option (quote_item_id);

create table if not exists idempotency_key (
    id uuid primary key,
    store_id uuid not null,
    scope varchar(100) not null,
    key_value varchar(128) not null,
    request_hash varchar(64) not null,
    response_body text null,
    http_status integer null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index if not exists uk_idempotency_key_store_scope_key
    on idempotency_key (store_id, scope, key_value);

create index if not exists ix_idempotency_key_expires_at
    on idempotency_key (expires_at);
