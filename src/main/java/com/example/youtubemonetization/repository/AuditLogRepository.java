package com.example.youtubemonetization.repository;

import com.example.youtubemonetization.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
