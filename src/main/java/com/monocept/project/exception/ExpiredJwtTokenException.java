package com.monocept.project.exception;

// FIX:
// Added dedicated exception for expired JWT token
// Covers EXC-014 Expired JWT token

public class ExpiredJwtTokenException extends RuntimeException {

    public ExpiredJwtTokenException(String message) {
        super(message);
    }
}