alter table delivery_zone
    add constraint chk_delivery_zone_fee_amount
        check (fee_amount >= 0);

alter table delivery_zone
    add constraint chk_delivery_zone_min_order_amount
        check (min_order_amount >= 0);

create index if not exists idx_delivery_zone_store_active
    on delivery_zone (store_id, active);
