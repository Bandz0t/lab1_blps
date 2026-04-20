package ru.ifmo.se.lab1.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModerateRequest {
    @NotNull
    private Long moderatorId;
}
