package com.example.youtubemonetization.dto.request;

import com.example.youtubemonetization.enums.ModerationDecisionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerationDecisionRequest {

    @NotNull(message = "Поле decision обязательно")
    private ModerationDecisionType decision;

    private String reason;
}
