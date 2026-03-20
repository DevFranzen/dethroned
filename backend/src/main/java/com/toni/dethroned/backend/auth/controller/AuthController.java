package com.toni.dethroned.backend.auth.controller;

import com.toni.dethroned.backend.auth.dto.AuthResponse;
import com.toni.dethroned.backend.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login() {
        String playerId = authService.generatePlayerId();
        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(playerId));
    }
}
