package com.monocept.project.exception;

// FIX:
// Added dedicated exception for invalid JWT token
// Covers EXC-013 Invalid JWT token

public class InvalidJwtTokenException extends RuntimeException {

    public InvalidJwtTokenException(String message) {
        super(message);
    }
}