package com.travelplan.auth.service;

import com.travelplan.auth.dto.*;
import com.travelplan.auth.entity.*;
import com.travelplan.auth.repository.*;
import com.travelplan.shared.dto.UserContext;
import com.travelplan.shared.exception.BusinessException;
import com.travelplan.shared.exception.ResourceNotFoundException;
import com.travelplan.shared.exception.UnauthorizedException;
import com.travelplan.shared.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailWithRoleAndPermissions(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessException("Account is not active", HttpStatus.FORBIDDEN);
        }

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email is already registered", HttpStatus.CONFLICT);
        }

        Role userRole = roleRepository.findByNameWithPermissions("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default user role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(userRole)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        User user = userRepository.findByIdWithRoleAndPermissions(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", refreshToken.getUserId()));

        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("User {} logged out, all refresh tokens revoked", userId);
    }

    @Transactional(readOnly = true)
    public UserContext validateToken(String token) {
        return jwtUtils.validateTokenAndGetUser(token);
    }

    private AuthResponse generateAuthResponse(User user) {
        UserContext userContext = buildUserContext(user);

        String accessToken = jwtUtils.generateAccessToken(userContext);
        String refreshToken = createRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().getName())
                        .permissions(getPermissionNames(user))
                        .build())
                .build();
    }

    private UserContext buildUserContext(User user) {
        return UserContext.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .permissions(getPermissionNames(user))
                .build();
    }

    private List<String> getPermissionNames(User user) {
        return user.getRole().getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
    }

    private String createRefreshToken(Long userId) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userId(userId)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
