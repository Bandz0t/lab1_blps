package ru.ifmo.se.lab1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.lab1.domain.Video;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
