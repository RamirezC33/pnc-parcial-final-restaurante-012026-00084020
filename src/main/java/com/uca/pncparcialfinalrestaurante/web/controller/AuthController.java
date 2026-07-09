package com.uca.pncparcialfinalrestaurante.web.controller;

import com.uca.pncparcialfinalrestaurante.security.CustomUserDetails;
import com.uca.pncparcialfinalrestaurante.service.AuthService;
import com.uca.pncparcialfinalrestaurante.web.dto.ChangePasswordRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.LoginRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.RefreshRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.RegisterRequest;
import com.uca.pncparcialfinalrestaurante.web.dto.TokenResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        var usuario = authService.register(request);
        return ResponseEntity.ok(Map.of(
                "id", usuario.getId(),
                "username", usuario.getUsername(),
                "role", usuario.getRole().name()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUsuario(), request);
        return ResponseEntity.ok(Map.of(
                "message", "Contraseña actualizada. Todas las sesiones anteriores fueron cerradas."));
    }
}
