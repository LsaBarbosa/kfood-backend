alter table store
    add column if not exists hours_version integer not null default 0;

alter table store_hours
    add constraint uk_store_hours_store_day unique (store_id, day_of_week);

alter table store_hours
    add constraint ck_store_hours_times
        check (
            (is_closed = true and open_time is null and close_time is null)
            or
            (is_closed = false and open_time is not null and close_time is not null and open_time < close_time)
        );
