package com.example.youtubemonetization.repository;

import com.example.youtubemonetization.entity.OutboxEvent;
import com.example.youtubemonetization.enums.OutboxEventStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
