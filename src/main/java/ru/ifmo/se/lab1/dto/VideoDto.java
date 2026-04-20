package ru.ifmo.se.lab1.dto;

import lombok.Data;
import ru.ifmo.se.lab1.domain.VideoStatus;
import ru.ifmo.se.lab1.domain.MonetizationStatus;
import java.time.LocalDateTime;

@Data
public class VideoDto {
    private Long id;
    private Long channelId;
    private String title;
    private String description;
    private VideoStatus status;
    private MonetizationStatus monetizationStatus;
    private Long viewsCount;
    private LocalDateTime createdAt;
}
