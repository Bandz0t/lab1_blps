package ru.ifmo.se.lab1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateVideoRequest {
    @NotBlank
    private String title;
    private String description;
}
