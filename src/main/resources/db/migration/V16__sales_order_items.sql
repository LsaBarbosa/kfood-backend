create table sales_order_item (
    id uuid primary key,
    order_id uuid not null,
    product_id uuid null,
    product_name_snapshot varchar(255) not null,
    unit_price_snapshot numeric(12, 2) not null,
    quantity integer not null,
    total_item_amount numeric(12, 2) not null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_sales_order_item_order
        foreign key (order_id) references sales_order (id) on delete cascade,
    constraint ck_sales_order_item_unit_price_non_negative
        check (unit_price_snapshot >= 0),
    constraint ck_sales_order_item_quantity_positive
        check (quantity > 0),
    constraint ck_sales_order_item_total_non_negative
        check (total_item_amount >= 0)
);

create table sales_order_item_option (
    id uuid primary key,
    order_item_id uuid not null,
    option_name_snapshot varchar(255) not null,
    extra_price_snapshot numeric(12, 2) not null,
    quantity integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_sales_order_item_option_order_item
        foreign key (order_item_id) references sales_order_item (id) on delete cascade,
    constraint ck_sales_order_item_option_extra_price_non_negative
        check (extra_price_snapshot >= 0),
    constraint ck_sales_order_item_option_quantity_positive
        check (quantity > 0)
);

create index ix_sales_order_item_order_id
    on sales_order_item (order_id);

create index ix_sales_order_item_product_id
    on sales_order_item (product_id);

create index ix_sales_order_item_option_order_item_id
    on sales_order_item_option (order_item_id);
