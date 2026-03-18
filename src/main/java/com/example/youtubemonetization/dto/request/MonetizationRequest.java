package com.example.youtubemonetization.dto.request;

import com.example.youtubemonetization.enums.MonetizationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonetizationRequest {

    @NotNull(message = "Monetization type is required")
    private MonetizationType monetizationType;
}
