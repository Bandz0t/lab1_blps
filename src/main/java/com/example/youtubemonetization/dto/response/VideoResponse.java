package com.example.youtubemonetization.dto.response;

import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.MonetizationType;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoResponse {

    private Long id;
    private Long authorId;
    private String title;
    private String description;
    private String filePath;
    private String format;
    private Long sizeBytes;
    private Integer durationSeconds;
    private UploadStatus uploadStatus;
    private ValidationStatus validationStatus;
    private CopyrightStatus copyrightStatus;
    private MonetizationStatus monetizationStatus;
    private MonetizationType monetizationType;
    private LocalDateTime publishedAt;
    private String processInstanceId;
}
