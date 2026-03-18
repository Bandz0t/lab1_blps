package com.example.youtubemonetization.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoCreateRequest {

    @NotNull
    private Long authorId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "File path is required")
    private String filePath;

    @NotBlank(message = "Format is required")
    private String format;

    @NotNull
    @Positive(message = "Size must be positive")
    private Long sizeBytes;

    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSeconds;
}
