package com.travelplan.user.controller;

import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.shared.dto.UserContext;
import com.travelplan.user.dto.*;
import com.travelplan.user.entity.User;
import com.travelplan.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Retrieve all users with optional filtering and pagination")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
            @Parameter(description = "Search term for name or email")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) User.UserStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.info("Fetching users with search: {}, status: {}", search, status);
        Page<UserDto> users = userService.getAllUsers(search, status, pageable);
        
        ApiResponse.PageInfo pageInfo = ApiResponse.PageInfo.builder()
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .hasNext(users.hasNext())
                .hasPrevious(users.hasPrevious())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(users, pageInfo));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Fetching user with id: {}", id);
        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user (Admin only)")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        UserDto user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update an existing user")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserContext userContext) {
        
        log.info("Updating user with id: {} by user: {}", id, userContext.getUserId());
        
        // Non-admin users cannot change status or role
        if (!userContext.isAdmin()) {
            request.setStatus(null);
            request.setRoleName(null);
        }
        
        UserDto user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete a user (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Deleting user with id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hard delete user", description = "Permanently delete a user and all related data")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        log.info("Hard deleting user with id: {}", id);
        userService.hardDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User permanently deleted"));
    }

    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.isOwner(#id, authentication)")
    @Operation(summary = "Add address", description = "Add an address to a user")
    public ResponseEntity<ApiResponse<UserDto.AddressDto>> addAddress(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UserDto.AddressDto addressDto) {
        log.info("Adding address for user: {}", id);
        UserDto.AddressDto address = userService.addAddress(id, addressDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(address, "Address added successfully"));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurityService.isOwner(#userId, authentication)")
    @Operation(summary = "Delete address", description = "Delete an address from a user")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Address ID") @PathVariable Long addressId) {
        log.info("Deleting address {} for user: {}", addressId, userId);
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(null, "Address deleted successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user statistics", description = "Get count of users by status")
    public ResponseEntity<ApiResponse<UserStats>> getUserStats() {
        UserStats stats = UserStats.builder()
                .activeUsers(userService.countUsersByStatus(User.UserStatus.ACTIVE))
                .inactiveUsers(userService.countUsersByStatus(User.UserStatus.INACTIVE))
                .suspendedUsers(userService.countUsersByStatus(User.UserStatus.SUSPENDED))
                .deletedUsers(userService.countUsersByStatus(User.UserStatus.DELETED))
                .build();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @lombok.Data
    @lombok.Builder
    public static class UserStats {
        private long activeUsers;
        private long inactiveUsers;
        private long suspendedUsers;
        private long deletedUsers;
    }
}
