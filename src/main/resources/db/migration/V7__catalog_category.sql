create table if not exists catalog_category (
    id uuid primary key,
    store_id uuid not null,
    name varchar(120) not null,
    sort_order integer not null default 0,
    active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_catalog_category_store
        foreign key (store_id) references store (id),
    constraint uk_catalog_category_store_name
        unique (store_id, name),
    constraint chk_catalog_category_sort_order
        check (sort_order >= 0)
);

create index if not exists idx_catalog_category_store_active_sort_order
    on catalog_category (store_id, active, sort_order);
