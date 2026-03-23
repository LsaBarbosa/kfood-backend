alter table payment
    add column if not exists expires_at timestamp with time zone;
