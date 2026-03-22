update sales_order
set payment_status_snapshot = 'PAID'
where payment_status_snapshot = 'CONFIRMED';

update sales_order
set payment_status_snapshot = 'FAILED'
where payment_status_snapshot in ('CANCELED', 'EXPIRED');

alter table sales_order
    add constraint ck_sales_order_payment_status_snapshot_v2
        check (payment_status_snapshot in ('PENDING', 'PAID', 'FAILED'));
