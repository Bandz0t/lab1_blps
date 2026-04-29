create table revenues (
    id bigserial primary key,
    video_id bigint not null references videos(id) on delete cascade,
    period_year integer not null,
    period_month integer not null,
    views_count bigint not null,
    monetized_views bigint not null,
    cpm numeric(10,2) not null,
    amount numeric(12,2) not null,
    currency varchar(10) not null,
    calculated_at timestamp not null,
    unique(video_id, period_year, period_month)
);

create index idx_revenues_video_id on revenues(video_id);
create index idx_revenues_period on revenues(period_year, period_month);
