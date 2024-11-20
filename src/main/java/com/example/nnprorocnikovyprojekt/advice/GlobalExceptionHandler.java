package com.example.nnprorocnikovyprojekt.advice;

import com.example.nnprorocnikovyprojekt.dtos.general.GeneralResponseDto;
import com.example.nnprorocnikovyprojekt.entity.User;
import com.example.nnprorocnikovyprojekt.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(basePackages = "com.example.nnprorocnikovyprojekt.controllers")
public class GlobalExceptionHandler {
    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GeneralResponseDto> handleAllExceptions(Exception ex, HttpServletRequest request) {
        String username = null;
        try {
            username = userService.getUserFromContext().getUsername();
        } catch (Exception e) {
            username = "username NOT in context";
        }

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String clientIP = request.getRemoteAddr();

        logger.error("An exception occurred at " + requestURI + " using method " + method);
        logger.error("Client IP: " + clientIP + ", user: " + username);

        return new ResponseEntity<>(
                new GeneralResponseDto("An error occurred at " + requestURI + " using method " + method + ": " + ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}