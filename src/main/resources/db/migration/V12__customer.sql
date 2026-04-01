create table if not exists customer (
    id uuid primary key,
    store_id uuid not null,
    name varchar(120) not null,
    phone varchar(20),
    email varchar(160),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_customer_store
        foreign key (store_id) references store (id),
    constraint uk_customer_store_phone
        unique (store_id, phone),
    constraint uk_customer_store_email
        unique (store_id, email),
    constraint chk_customer_phone_or_email
        check (phone is not null or email is not null)
);

create index if not exists idx_customer_store_phone
    on customer (store_id, phone);

create index if not exists idx_customer_store_email
    on customer (store_id, email);
