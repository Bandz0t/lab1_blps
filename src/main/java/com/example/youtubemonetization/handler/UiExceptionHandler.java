package com.example.youtubemonetization.handler;

import com.example.youtubemonetization.exception.BusinessException;
import com.example.youtubemonetization.exception.ConflictException;
import com.example.youtubemonetization.exception.EntityNotFoundException;
import com.example.youtubemonetization.exception.IllegalProcessStateException;
import com.example.youtubemonetization.exception.RequestValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(basePackages = "com.example.youtubemonetization.controller.ui")
public class UiExceptionHandler {

    private static final DateTimeFormatter UI_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @ExceptionHandler(EntityNotFoundException.class)
    public ModelAndView handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return buildErrorPage(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({
            RequestValidationException.class,
            IllegalArgumentException.class
    })
    public ModelAndView handleValidation(RuntimeException ex, HttpServletRequest request) {
        return buildErrorPage(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({
            BusinessException.class,
            IllegalProcessStateException.class,
            ConflictException.class,
            IllegalStateException.class
    })
    public ModelAndView handleBusiness(RuntimeException ex, HttpServletRequest request) {
        return buildErrorPage(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorPage(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex, HttpServletRequest request) {
        return buildErrorPage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Произошла непредвиденная ошибка. Попробуйте повторить действие позже.",
                request
        );
    }

    private ModelAndView buildErrorPage(HttpStatus status, String message, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("error", status.getReasonPhrase());
        modelAndView.addObject("message", message);
        modelAndView.addObject("path", request.getRequestURI());
        modelAndView.addObject("timestamp", LocalDateTime.now().format(UI_TIME_FORMATTER));
        return modelAndView;
    }
}
