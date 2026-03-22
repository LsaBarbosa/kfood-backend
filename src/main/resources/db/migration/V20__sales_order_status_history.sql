create table sales_order_status_history (
    id uuid primary key,
    store_id uuid not null,
    order_id uuid not null,
    previous_status varchar(30) not null,
    new_status varchar(30) not null,
    actor_user_id uuid not null,
    reason varchar(255),
    changed_at timestamp with time zone not null,
    constraint fk_sales_order_status_history_store
        foreign key (store_id) references store (id),
    constraint fk_sales_order_status_history_order
        foreign key (order_id) references sales_order (id) on delete cascade,
    constraint ck_sales_order_status_history_status_diff
        check (previous_status <> new_status)
);

create index ix_sales_order_status_history_order_changed_at
    on sales_order_status_history (order_id, changed_at desc);

create index ix_sales_order_status_history_store_changed_at
    on sales_order_status_history (store_id, changed_at desc);
