package ru.ifmo.se.lab1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.ifmo.se.lab1.domain.Role;

@Data
public class CreateUserRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotNull
    private Role role;
}
