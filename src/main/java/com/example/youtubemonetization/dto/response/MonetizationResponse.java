package com.example.youtubemonetization.dto.response;

import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.MonetizationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonetizationResponse {

    private Long videoId;
    private MonetizationType monetizationType;
    private MonetizationStatus monetizationStatus;
    private boolean published;
}
