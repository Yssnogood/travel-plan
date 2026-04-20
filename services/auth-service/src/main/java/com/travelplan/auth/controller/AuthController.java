package com.travelplan.auth.controller;

import com.travelplan.auth.dto.*;
import com.travelplan.auth.service.AuthService;
import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.shared.dto.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registration successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and revoke all refresh tokens")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserContext userContext) {
        log.info("Logout request for user: {}", userContext.getUserId());
        authService.logout(userContext.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get details of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserContext>> getCurrentUser(
            @AuthenticationPrincipal UserContext userContext) {
        return ResponseEntity.ok(ApiResponse.success(userContext));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Validate JWT token and return user context")
    public ResponseEntity<ApiResponse<UserContext>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UserContext userContext = authService.validateToken(token);
        if (userContext == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid token"));
        }
        return ResponseEntity.ok(ApiResponse.success(userContext));
    }
}
