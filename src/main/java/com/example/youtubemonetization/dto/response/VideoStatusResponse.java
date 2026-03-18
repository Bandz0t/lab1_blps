package com.example.youtubemonetization.dto.response;

import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoStatusResponse {

    private Long videoId;
    private String processInstanceId;
    private UploadStatus uploadStatus;
    private ValidationStatus validationStatus;
    private CopyrightStatus copyrightStatus;
    private MonetizationStatus monetizationStatus;
    private String currentStep;
    private boolean published;
}
