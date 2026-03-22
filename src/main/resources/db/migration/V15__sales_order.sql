create table sales_order (
    id uuid primary key,
    store_id uuid not null,
    customer_id uuid not null,
    order_number varchar(50),
    status varchar(30) not null,
    fulfillment_type varchar(20) not null,
    subtotal_amount numeric(12, 2) not null,
    delivery_fee_amount numeric(12, 2) not null,
    total_amount numeric(12, 2) not null,
    scheduled_for timestamp with time zone null,
    notes varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_sales_order_store
        foreign key (store_id) references store (id),
    constraint fk_sales_order_customer
        foreign key (customer_id) references customer (id),
    constraint ck_sales_order_subtotal_non_negative
        check (subtotal_amount >= 0),
    constraint ck_sales_order_delivery_fee_non_negative
        check (delivery_fee_amount >= 0),
    constraint ck_sales_order_total_non_negative
        check (total_amount >= 0),
    constraint ck_sales_order_total_consistency
        check (total_amount = subtotal_amount + delivery_fee_amount)
);

create unique index uk_sales_order_order_number
    on sales_order (order_number);

create index ix_sales_order_store_id
    on sales_order (store_id);

create index ix_sales_order_customer_id
    on sales_order (customer_id);

create index ix_sales_order_store_status_created_at
    on sales_order (store_id, status, created_at desc);
