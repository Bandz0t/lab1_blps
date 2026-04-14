create table if not exists moderation_requests (
    id bigserial primary key,
    video_id bigint not null references videos(id) on delete cascade,
    status varchar(50) not null,
    reason text,
    moderator_id bigint references users(id),
    active boolean not null default true,
    decided_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index if not exists idx_moderation_requests_video_active
    on moderation_requests(video_id, active);

create table if not exists notifications (
    id bigserial primary key,
    recipient_id bigint not null references users(id) on delete cascade,
    video_id bigint references videos(id) on delete set null,
    message text not null,
    is_read boolean not null default false,
    created_at timestamp not null
);

create index if not exists idx_notifications_recipient_id on notifications(recipient_id);

create table if not exists audit_logs (
    id bigserial primary key,
    actor_id bigint,
    actor_username varchar(100) not null,
    action varchar(100) not null,
    entity_type varchar(100) not null,
    entity_id bigint not null,
    details text,
    created_at timestamp not null
);

create index if not exists idx_audit_logs_entity on audit_logs(entity_type, entity_id);
