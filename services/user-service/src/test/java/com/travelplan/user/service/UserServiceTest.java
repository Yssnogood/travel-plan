package com.travelplan.user.service;

import com.travelplan.shared.exception.BusinessException;
import com.travelplan.shared.exception.ResourceNotFoundException;
import com.travelplan.user.dto.CreateUserRequest;
import com.travelplan.user.dto.UpdateUserRequest;
import com.travelplan.user.dto.UserDto;
import com.travelplan.user.entity.User;
import com.travelplan.user.entity.UserAddress;
import com.travelplan.user.repository.UserAddressRepository;
import com.travelplan.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAddressRepository addressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("+33612345678")
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateUserRequest.builder()
                .email("new@example.com")
                .password("password123")
                .firstName("Jane")
                .lastName("Smith")
                .phone("+33698765432")
                .build();

        updateRequest = UpdateUserRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .build();
    }

    @Test
    void getAllUsers_ShouldReturnPagedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

        when(userRepository.findAllWithFilters(null, null, pageable)).thenReturn(userPage);
        when(addressRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());

        Page<UserDto> result = userService.getAllUsers(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findAllWithFilters(null, null, pageable);
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        UserDto result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserById_WhenUserNotFound_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createUser_ShouldCreateAndReturnUser() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(addressRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());

        UserDto result = userService.createUser(createRequest);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFirstName()).isEqualTo("Jane");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WhenEmailExists_ShouldThrowException() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateUser_ShouldUpdateAndReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(addressRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        UserDto result = userService.updateUser(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_ShouldSoftDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.deleteUser(1L);

        verify(userRepository).save(argThat(user -> user.getStatus() == User.UserStatus.DELETED));
    }

    @Test
    void deleteUser_WhenNotFound_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllUsers_WithSearch_ShouldFilterUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);

        when(userRepository.findAllWithFilters("john", null, pageable)).thenReturn(userPage);
        when(addressRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());

        Page<UserDto> result = userService.getAllUsers("john", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findAllWithFilters("john", null, pageable);
    }
}
