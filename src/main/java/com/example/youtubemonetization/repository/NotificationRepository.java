package com.example.youtubemonetization.repository;

import com.example.youtubemonetization.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
