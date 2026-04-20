package ru.ifmo.se.lab1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.lab1.domain.Channel;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
}
