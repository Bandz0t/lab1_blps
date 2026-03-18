package com.example.youtubemonetization.dto.request;

import com.example.youtubemonetization.enums.ClaimType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CopyrightCheckRequest {

    private boolean hasViolation;
    private ClaimType claimType;
    private String description;
    private String detectedFragment;
}
