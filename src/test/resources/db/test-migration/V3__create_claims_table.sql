create table claims (
    id bigserial primary key,
    video_id bigint not null references videos(id) on delete cascade,
    claim_type varchar(50) not null,
    description text,
    detected_fragment varchar(255),
    status varchar(50) not null,
    created_at timestamp not null,
    resolved_at timestamp
);

create index idx_claims_video_id on claims(video_id);
create index idx_claims_status on claims(status);
