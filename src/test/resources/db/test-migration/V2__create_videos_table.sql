create table videos (
    id bigserial primary key,
    title varchar(200) not null,
    description text,
    file_path varchar(255) not null,
    format varchar(20) not null,
    size_bytes bigint not null,
    duration_seconds integer,
    upload_status varchar(50) not null,
    validation_status varchar(50) not null,
    copyright_status varchar(50) not null,
    monetization_status varchar(50) not null,
    monetization_type varchar(50) not null,
    process_instance_id varchar(100),
    published_at timestamp,
    author_id bigint not null references users(id),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_videos_author_id on videos(author_id);
