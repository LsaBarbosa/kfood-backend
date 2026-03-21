alter table catalog_product
    add constraint uk_catalog_product_store_id_id unique (store_id, id);

create table if not exists catalog_option_group (
    id uuid primary key,
    store_id uuid not null,
    product_id uuid not null,
    name varchar(120) not null,
    min_select integer not null default 0,
    max_select integer not null default 1,
    required boolean not null default false,
    active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_catalog_option_group_product_store
        foreign key (store_id, product_id) references catalog_product (store_id, id),
    constraint chk_catalog_option_group_min_select
        check (min_select >= 0),
    constraint chk_catalog_option_group_max_select
        check (max_select >= min_select)
);

create index if not exists idx_catalog_option_group_store_product
    on catalog_option_group (store_id, product_id);
