create index if not exists ix_sales_order_store_scheduled_for
    on sales_order (store_id, scheduled_for);

create index if not exists ix_sales_order_status_scheduled_for
    on sales_order (status, scheduled_for);
