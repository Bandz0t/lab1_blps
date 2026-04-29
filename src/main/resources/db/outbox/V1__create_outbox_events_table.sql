create table if not exists outbox_events (
    id bigserial primary key,
    event_type varchar(100) not null,
    aggregate_type varchar(100) not null,
    aggregate_id bigint,
    channel varchar(150) not null,
    payload text not null,
    status varchar(20) not null,
    attempts int not null default 0,
    error text,
    created_at timestamp not null,
    locked_until timestamp,
    sent_at timestamp
);

create index if not exists idx_outbox_events_claim
    on outbox_events(status, locked_until, created_at);
