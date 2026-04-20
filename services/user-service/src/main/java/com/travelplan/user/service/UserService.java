package com.travelplan.user.service;

import com.travelplan.shared.dto.ApiResponse;
import com.travelplan.shared.exception.BusinessException;
import com.travelplan.shared.exception.ResourceNotFoundException;
import com.travelplan.user.dto.*;
import com.travelplan.user.entity.User;
import com.travelplan.user.entity.UserAddress;
import com.travelplan.user.repository.UserAddressRepository;
import com.travelplan.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(String search, User.UserStatus status, Pageable pageable) {
        Page<User> users = userRepository.findAllWithFilters(search, status, pageable);
        return users.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return mapToDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email is already in use", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .avatarUrl(request.getAvatarUrl())
                .status(User.UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("Created new user with id: {}", user.getId());
        return mapToDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        user = userRepository.save(user);
        log.info("Updated user with id: {}", user.getId());
        return mapToDto(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Soft delete by setting status to DELETED
        user.setStatus(User.UserStatus.DELETED);
        userRepository.save(user);
        
        log.info("Deleted user with id: {}", id);
    }

    @Transactional
    public void hardDeleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        
        // Delete addresses first (cascade)
        addressRepository.deleteByUserId(id);
        userRepository.deleteById(id);
        
        log.info("Hard deleted user with id: {}", id);
    }

    @Transactional
    public UserDto.AddressDto addAddress(Long userId, UserDto.AddressDto addressDto) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }

        // If this is set as primary, unset other primary addresses
        if (Boolean.TRUE.equals(addressDto.getIsPrimary())) {
            addressRepository.unsetPrimaryAddresses(userId);
        }

        UserAddress address = UserAddress.builder()
                .userId(userId)
                .street(addressDto.getStreet())
                .city(addressDto.getCity())
                .state(addressDto.getState())
                .country(addressDto.getCountry())
                .postalCode(addressDto.getPostalCode())
                .isPrimary(addressDto.getIsPrimary())
                .build();

        address = addressRepository.save(address);
        return mapAddressToDto(address);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("Address does not belong to this user", HttpStatus.FORBIDDEN);
        }

        addressRepository.delete(address);
    }

    @Transactional(readOnly = true)
    public long countUsersByStatus(User.UserStatus status) {
        return userRepository.countByStatus(status);
    }

    private UserDto mapToDto(User user) {
        List<UserAddress> addresses = addressRepository.findByUserId(user.getId());
        
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .addresses(addresses.stream().map(this::mapAddressToDto).collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private UserDto.AddressDto mapAddressToDto(UserAddress address) {
        return UserDto.AddressDto.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .isPrimary(address.getIsPrimary())
                .build();
    }
}
