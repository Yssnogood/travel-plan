package com.travelplan.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.travelplan.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private User.UserStatus status;
    private Boolean emailVerified;
    private String roleName;
    private List<AddressDto> addresses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private Long id;
        private String street;
        private String city;
        private String state;
        private String country;
        private String postalCode;
        private Boolean isPrimary;
    }
}
