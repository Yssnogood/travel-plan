package com.travelplan.user.security;

import com.travelplan.shared.dto.UserContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("userSecurityService")
public class UserSecurityService {

    public boolean isOwner(Long userId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserContext userContext) {
            return userId.equals(userContext.getUserId());
        }
        
        return false;
    }
}
