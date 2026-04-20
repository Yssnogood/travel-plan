package com.travelplan.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown for unauthorized access attempts
 */
public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException() {
        super("Unauthorized access", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
    
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
