create table payouts (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    period_year integer not null,
    period_month integer not null,
    total_amount numeric(12,2) not null,
    status varchar(20) not null,
    processed_at timestamp,
    created_at timestamp not null,
    unique(user_id, period_year, period_month)
);

create index idx_payouts_user_id on payouts(user_id);
create index idx_payouts_status on payouts(status);
