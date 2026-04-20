package ru.ifmo.se.lab1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChannelRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String name;

    private String description;
}
