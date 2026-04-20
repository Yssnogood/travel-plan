package com.travelplan.auth.service;

import com.travelplan.auth.dto.*;
import com.travelplan.auth.entity.*;
import com.travelplan.auth.repository.*;
import com.travelplan.shared.exception.BusinessException;
import com.travelplan.shared.exception.UnauthorizedException;
import com.travelplan.shared.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        testPermission = Permission.builder()
                .id(1L)
                .name("users:read")
                .resource("users")
                .action("read")
                .build();

        testRole = Role.builder()
                .id(1L)
                .name("USER")
                .permissions(Set.of(testPermission))
                .build();

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(testRole)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            // Given
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            when(userRepository.findByEmailWithRoleAndPermissions("test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword"))
                    .thenReturn(true);
            when(jwtUtils.generateAccessToken(any()))
                    .thenReturn("access-token");

            // When
            AuthResponse response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid email")
        void shouldThrowExceptionForInvalidEmail() {
            // Given
            LoginRequest request = LoginRequest.builder()
                    .email("invalid@example.com")
                    .password("password123")
                    .build();

            when(userRepository.findByEmailWithRoleAndPermissions("invalid@example.com"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw exception for invalid password")
        void shouldThrowExceptionForInvalidPassword() {
            // Given
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("wrongPassword")
                    .build();

            when(userRepository.findByEmailWithRoleAndPermissions("test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "hashedPassword"))
                    .thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw exception for inactive account")
        void shouldThrowExceptionForInactiveAccount() {
            // Given
            testUser.setStatus(User.UserStatus.SUSPENDED);
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            when(userRepository.findByEmailWithRoleAndPermissions("test@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashedPassword"))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Account is not active");
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register successfully")
        void shouldRegisterSuccessfully() {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("new@example.com")
                    .password("password123")
                    .firstName("Jane")
                    .lastName("Doe")
                    .build();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(roleRepository.findByNameWithPermissions("USER")).thenReturn(Optional.of(testRole));
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User user = i.getArgument(0);
                user.setId(2L);
                return user;
            });
            when(jwtUtils.generateAccessToken(any())).thenReturn("access-token");

            // When
            AuthResponse response = authService.register(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getUser().getEmail()).isEqualTo("new@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception for duplicate email")
        void shouldThrowExceptionForDuplicateEmail() {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .firstName("Jane")
                    .lastName("Doe")
                    .build();

            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Email is already registered");
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .token("valid-refresh-token")
                    .userId(1L)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .revoked(false)
                    .build();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("valid-refresh-token")
                    .build();

            when(refreshTokenRepository.findByToken("valid-refresh-token"))
                    .thenReturn(Optional.of(refreshToken));
            when(userRepository.findByIdWithRoleAndPermissions(1L))
                    .thenReturn(Optional.of(testUser));
            when(jwtUtils.generateAccessToken(any()))
                    .thenReturn("new-access-token");

            // When
            AuthResponse response = authService.refreshToken(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            verify(refreshTokenRepository).save(argThat(token -> token.getRevoked()));
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            // Given
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalid-token")
                    .build();

            when(refreshTokenRepository.findByToken("invalid-token"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("Should throw exception for expired refresh token")
        void shouldThrowExceptionForExpiredRefreshToken() {
            // Given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(1L)
                    .token("expired-token")
                    .userId(1L)
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .revoked(false)
                    .build();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("expired-token")
                    .build();

            when(refreshTokenRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(refreshToken));

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Refresh token is expired or revoked");
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() {
            // When
            authService.logout(1L);

            // Then
            verify(refreshTokenRepository).revokeAllByUserId(1L);
        }
    }
}
