package ru.ifmo.se.lab1.dto;

import lombok.Data;
import java.math.BigDecimal;
import ru.ifmo.se.lab1.domain.Role;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private BigDecimal balance;
}
