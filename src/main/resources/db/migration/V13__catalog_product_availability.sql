create table if not exists catalog_product_availability (
    id uuid primary key,
    product_id uuid not null,
    day_of_week varchar(20) not null,
    start_time time not null,
    end_time time not null,
    active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_catalog_product_availability_product
        foreign key (product_id) references catalog_product (id),
    constraint chk_catalog_product_availability_time_range
        check (start_time < end_time)
);

create index if not exists idx_catalog_product_availability_product
    on catalog_product_availability (product_id);

create index if not exists idx_catalog_product_availability_product_day_active
    on catalog_product_availability (product_id, day_of_week, active);
