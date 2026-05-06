package com.example.youtubemonetization.repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SchedulerLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public SchedulerLockRepository(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tryAcquire(String lockName, String ownerId, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = now.plus(ttl);
        int updated = jdbcTemplate.update("""
                update scheduler_locks
                set owner_id = ?, locked_until = ?, updated_at = ?
                where lock_name = ? and locked_until < ?
                """, ownerId, Timestamp.valueOf(lockedUntil), Timestamp.valueOf(now), lockName, Timestamp.valueOf(now));
        if (updated == 1) {
            return true;
        }
        try {
            jdbcTemplate.update("""
                    insert into scheduler_locks(lock_name, owner_id, locked_until, updated_at)
                    values (?, ?, ?, ?)
                    """, lockName, ownerId, Timestamp.valueOf(lockedUntil), Timestamp.valueOf(now));
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }
}
