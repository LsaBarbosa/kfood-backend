create table if not exists catalog_option_item (
    id uuid primary key,
    option_group_id uuid not null,
    name varchar(120) not null,
    extra_price numeric(12, 2) not null,
    active boolean not null default true,
    sort_order integer not null default 0,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_catalog_option_item_group
        foreign key (option_group_id) references catalog_option_group (id),
    constraint chk_catalog_option_item_extra_price
        check (extra_price >= 0),
    constraint chk_catalog_option_item_sort_order
        check (sort_order >= 0)
);

create index if not exists idx_catalog_option_item_group_id
    on catalog_option_item (option_group_id);

create index if not exists idx_catalog_option_item_group_sort
    on catalog_option_item (option_group_id, sort_order, id);
