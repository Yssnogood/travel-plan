package com.travelplan.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * User context information extracted from JWT token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private List<String> permissions;
    
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    public boolean hasRole(String roleName) {
        return role != null && role.equalsIgnoreCase(roleName);
    }
    
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    public boolean isManager() {
        return hasRole("MANAGER") || isAdmin();
    }
}
