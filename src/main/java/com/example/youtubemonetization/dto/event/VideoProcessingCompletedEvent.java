package com.example.youtubemonetization.dto.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProcessingCompletedEvent {

    private Long videoId;
    private String uploadStatus;
    private String validationStatus;
    private String copyrightStatus;
    private String processedByNode;
    private LocalDateTime processedAt;
}
