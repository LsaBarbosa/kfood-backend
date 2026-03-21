create index if not exists idx_catalog_category_store_active_sort_name
    on catalog_category (store_id, active, sort_order, name);

create index if not exists idx_catalog_product_store_active_paused_sort_name
    on catalog_product (store_id, active, paused, sort_order, name);
