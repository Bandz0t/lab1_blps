package ru.ifmo.se.lab1.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChannelDto {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
