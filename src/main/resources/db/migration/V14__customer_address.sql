create table if not exists customer_address (
    id uuid primary key,
    customer_id uuid not null,
    label varchar(60) not null,
    zip_code varchar(8) not null,
    street varchar(160) not null,
    number varchar(20) not null,
    district varchar(100) not null,
    city varchar(100) not null,
    state varchar(2) not null,
    complement varchar(120),
    main_address boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_customer_address_customer
        foreign key (customer_id) references customer (id) on delete cascade
);

create index if not exists idx_customer_address_customer
    on customer_address (customer_id);

create index if not exists idx_customer_address_customer_main
    on customer_address (customer_id, main_address);
