package com.travelplan.shared.security;

import com.travelplan.shared.dto.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT Authentication filter to validate and process JWT tokens
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt)) {
                UserContext userContext = jwtUtils.validateTokenAndGetUser(jwt);

                if (userContext != null) {
                    List<SimpleGrantedAuthority> authorities = buildAuthorities(userContext);

                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                    userContext, 
                                    null, 
                                    authorities);
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Add user context to request attributes for easy access
                    request.setAttribute("userContext", userContext);
                }
            }
        } catch (Exception ex) {
            log.error("Cannot set user authentication: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private List<SimpleGrantedAuthority> buildAuthorities(UserContext userContext) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // Add role authority
        if (userContext.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + userContext.getRole()));
        }
        
        // Add permission authorities
        if (userContext.getPermissions() != null) {
            userContext.getPermissions().forEach(permission -> 
                    authorities.add(new SimpleGrantedAuthority(permission)));
        }
        
        return authorities;
    }
}
