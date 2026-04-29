package com.example.youtubemonetization.repository;

import com.example.youtubemonetization.entity.OutboxEvent;
import com.example.youtubemonetization.enums.OutboxEventStatus;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class OutboxEventJdbcRepository {

    private static final RowMapper<OutboxEvent> ROW_MAPPER = (rs, rowNum) -> {
        OutboxEvent event = new OutboxEvent();
        event.setId(rs.getLong("id"));
        event.setEventType(rs.getString("event_type"));
        event.setAggregateType(rs.getString("aggregate_type"));
        event.setAggregateId(rs.getObject("aggregate_id", Long.class));
        event.setChannel(rs.getString("channel"));
        event.setPayload(rs.getString("payload"));
        event.setStatus(OutboxEventStatus.valueOf(rs.getString("status")));
        event.setAttempts(rs.getInt("attempts"));
        event.setError(rs.getString("error"));
        event.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        event.setLockedUntil(toLocalDateTime(rs.getTimestamp("locked_until")));
        event.setSentAt(toLocalDateTime(rs.getTimestamp("sent_at")));
        return event;
    };

    private final JdbcTemplate jdbcTemplate;
    private Boolean postgres;

    public OutboxEventJdbcRepository(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(OutboxEvent event) {
        LocalDateTime now = LocalDateTime.now();
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(now);
        }
        if (event.getAttempts() == null) {
            event.setAttempts(0);
        }
        if (event.getStatus() == null) {
            event.setStatus(OutboxEventStatus.NEW);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into outbox_events (
                        event_type,
                        aggregate_type,
                        aggregate_id,
                        channel,
                        payload,
                        status,
                        attempts,
                        error,
                        created_at,
                        locked_until,
                        sent_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[] {"id"});
            statement.setString(1, event.getEventType());
            statement.setString(2, event.getAggregateType());
            statement.setObject(3, event.getAggregateId());
            statement.setString(4, event.getChannel());
            statement.setString(5, event.getPayload());
            statement.setString(6, event.getStatus().name());
            statement.setInt(7, event.getAttempts());
            statement.setString(8, event.getError());
            statement.setTimestamp(9, Timestamp.valueOf(event.getCreatedAt()));
            statement.setTimestamp(10, toTimestamp(event.getLockedUntil()));
            statement.setTimestamp(11, toTimestamp(event.getSentAt()));
            return statement;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            event.setId(keyHolder.getKey().longValue());
        }
    }

    public Optional<OutboxEvent> findById(Long id) {
        List<OutboxEvent> events = jdbcTemplate.query(
                "select * from outbox_events where id = ?",
                ROW_MAPPER,
                id
        );
        return events.stream().findFirst();
    }

    public List<OutboxEvent> claimPending(int limit, Duration lockTtl) {
        if (isPostgres()) {
            return claimPendingPostgres(limit, lockTtl);
        }
        return claimPendingPortable(limit, lockTtl);
    }

    public void markSent(Long id) {
        jdbcTemplate.update("""
                update outbox_events
                set status = ?, error = null, locked_until = null, sent_at = ?
                where id = ?
                """, OutboxEventStatus.SENT.name(), Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public void markFailed(Long id, String errorMessage) {
        jdbcTemplate.update("""
                update outbox_events
                set status = ?, error = ?, attempts = attempts + 1, locked_until = null
                where id = ?
                """, OutboxEventStatus.FAILED.name(), errorMessage, id);
    }

    private List<OutboxEvent> claimPendingPostgres(int limit, Duration lockTtl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = now.plus(lockTtl);
        return jdbcTemplate.query("""
                with claimed as (
                    select id
                    from outbox_events
                    where status in ('NEW', 'FAILED')
                       or (status = 'PROCESSING' and locked_until < ?)
                    order by created_at asc
                    limit ?
                    for update skip locked
                )
                update outbox_events event
                set status = ?, locked_until = ?
                from claimed
                where event.id = claimed.id
                returning event.*
                """, ROW_MAPPER, Timestamp.valueOf(now), limit, OutboxEventStatus.PROCESSING.name(), Timestamp.valueOf(lockedUntil));
    }

    private List<OutboxEvent> claimPendingPortable(int limit, Duration lockTtl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = now.plus(lockTtl);
        List<Long> ids = jdbcTemplate.queryForList("""
                select id
                from outbox_events
                where status in ('NEW', 'FAILED')
                   or (status = 'PROCESSING' and locked_until < ?)
                order by created_at asc
                limit ?
                """, Long.class, Timestamp.valueOf(now), limit);
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Object[] updateArgs = new Object[ids.size() + 2];
        updateArgs[0] = OutboxEventStatus.PROCESSING.name();
        updateArgs[1] = Timestamp.valueOf(lockedUntil);
        for (int i = 0; i < ids.size(); i++) {
            updateArgs[i + 2] = ids.get(i);
        }
        jdbcTemplate.update(
                "update outbox_events set status = ?, locked_until = ? where id in (" + placeholders + ")",
                updateArgs
        );
        return jdbcTemplate.query(
                "select * from outbox_events where id in (" + placeholders + ") order by created_at asc",
                ROW_MAPPER,
                ids.toArray()
        );
    }

    private boolean isPostgres() {
        if (postgres == null) {
            postgres = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                    connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("postgres"));
            log.debug("Outbox datasource PostgreSQL mode: {}", postgres);
        }
        return postgres;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
