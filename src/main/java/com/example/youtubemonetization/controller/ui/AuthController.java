package com.example.youtubemonetization.controller.ui;

import com.example.youtubemonetization.dto.request.auth.RegisterRequest;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.repository.RoleRepository;
import com.example.youtubemonetization.security.AuthenticatedUser;
import com.example.youtubemonetization.security.JwtAuthenticationFilter;
import com.example.youtubemonetization.service.AuthService;
import com.example.youtubemonetization.service.UserDataService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String login(String username, String password, HttpServletResponse response, Model model) {
        try {
            AuthenticatedUser user = authService.authenticate(username, password);
            addAuthCookie(response, authService.issueToken(user), 24 * 60 * 60);
            return "redirect:/dashboard";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", "Неверный логин или пароль");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        addAuthCookie(response, "", 0);
        return "redirect:/login?logout";
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
        roleRepository.findByName("AUTHOR").ifPresent(role -> user.getRoles().add(role));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userDataService.create(user);

        model.addAttribute("successMessage", "Регистрация завершена. Войдите в аккаунт.");
        return "login";
    }

    private void addAuthCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
