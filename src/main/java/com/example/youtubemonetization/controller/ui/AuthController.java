package com.example.youtubemonetization.controller.ui;

import com.example.youtubemonetization.dto.request.auth.RegisterRequest;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.service.UserDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserDataService userDataService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerForm") RegisterRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (userDataService.findByUsername(request.getUsername()).isPresent()) {
            bindingResult.rejectValue("username", "exists", "Логин уже занят");
        }
        if (userDataService.findByEmail(request.getEmail()).isPresent()) {
            bindingResult.rejectValue("email", "exists", "Email уже зарегистрирован");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setChannelName(request.getChannelName());
        user.setRole("AUTHOR");
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userDataService.create(user);

        model.addAttribute("successMessage", "Регистрация завершена. Войдите в аккаунт.");
        return "login";
    }
}
