package com.travelplan.userservice.service;

import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.userservice.dto.CreateUserRequest;
import com.travelplan.userservice.dto.UpdateUserRequest;
import com.travelplan.userservice.dto.UserDto;
import com.travelplan.userservice.entity.User;
import com.travelplan.userservice.repository.UserRepository;
import com.travelplan.shared.exception.ResourceNotFoundException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private CreateUserRequest createRequest;
    private UpdateUserRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhoneNumber("+33612345678");
        testUser.setDateOfBirth(LocalDate.of(1990, 1, 15));
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        createRequest = new CreateUserRequest();
        createRequest.setEmail("new@example.com");
        createRequest.setPassword("password123");
        createRequest.setFirstName("Jane");
        createRequest.setLastName("Smith");

        updateRequest = new UpdateUserRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
    }

    @Test
    void getAllUsers_ShouldReturnPagedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
        
        when(userRepository.findAllActive(pageable)).thenReturn(userPage);

        ApiResponse<List<UserDto>> response = userService.getAllUsers(0, 10, null, null, null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findAllActive(pageable);
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testUser));

        ApiResponse<UserDto> response = userService.getUserById(1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getId()).isEqualTo(1L);
        assertThat(response.getData().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getUserById_WhenUserNotFound_ShouldThrowException() {
        when(userRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
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

        ApiResponse<UserDto> response = userService.createUser(createRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getEmail()).isEqualTo("new@example.com");
        assertThat(response.getData().getFirstName()).isEqualTo("Jane");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_WhenEmailExists_ShouldThrowException() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void updateUser_ShouldUpdateAndReturnUser() {
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        ApiResponse<UserDto> response = userService.updateUser(1L, updateRequest);

        assertThat(response.isSuccess()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deactivateUser_ShouldSetActiveToFalse() {
        when(userRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        ApiResponse<UserDto> response = userService.deactivateUser(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(userRepository).save(argThat(user -> !user.isActive()));
    }

    @Test
    void deleteUser_ShouldCallRepositoryDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(any(User.class));

        ApiResponse<Void> response = userService.deleteUser(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(userRepository).delete(testUser);
    }

    @Test
    void searchUsers_ShouldReturnMatchingUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
        
        when(userRepository.searchUsers("john", pageable)).thenReturn(userPage);

        ApiResponse<List<UserDto>> response = userService.getAllUsers(0, 10, "john", null, null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        verify(userRepository).searchUsers("john", pageable);
    }
}
