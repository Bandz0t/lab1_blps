package ru.ifmo.se.lab1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.lab1.domain.User;
import ru.ifmo.se.lab1.dto.CreateUserRequest;
import ru.ifmo.se.lab1.dto.UserDto;
import ru.ifmo.se.lab1.exception.BusinessRuleException;
import ru.ifmo.se.lab1.exception.ResourceNotFoundException;
import ru.ifmo.se.lab1.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setRole(request.getRole());
            user = userRepository.save(user);
            return mapToDto(user);
        } catch (Exception e) {
            throw new BusinessRuleException("Username or email already exists");
        }
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return mapToDto(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    public User getDomainUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public void addBalance(Long userId, java.math.BigDecimal amount) {
        User user = getDomainUser(userId);
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);
    }

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setBalance(user.getBalance());
        return dto;
    }
}
