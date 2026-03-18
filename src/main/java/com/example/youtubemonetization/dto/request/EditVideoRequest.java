package com.example.youtubemonetization.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditVideoRequest {

    @NotBlank(message = "New file path is required")
    private String newFilePath;

    private String comment;
}
