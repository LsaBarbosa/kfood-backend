alter table catalog_category
    add constraint uk_catalog_category_store_id_id unique (store_id, id);

create table if not exists catalog_product (
    id uuid primary key,
    store_id uuid not null,
    category_id uuid not null,
    name varchar(160) not null,
    description varchar(500) not null,
    base_price numeric(12, 2) not null,
    image_url varchar(500),
    sort_order integer not null default 0,
    active boolean not null default true,
    paused boolean not null default false,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_catalog_product_store
        foreign key (store_id) references store (id),
    constraint fk_catalog_product_category_store
        foreign key (store_id, category_id) references catalog_category (store_id, id),
    constraint chk_catalog_product_base_price
        check (base_price >= 0),
    constraint chk_catalog_product_sort_order
        check (sort_order >= 0)
);

create index if not exists idx_catalog_product_store_active_paused_sort_order
    on catalog_product (store_id, active, paused, sort_order);
create index if not exists idx_catalog_product_category_id
    on catalog_product (category_id);
