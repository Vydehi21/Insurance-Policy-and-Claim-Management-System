package com.monocept.project.exception;

public class AuthenticationException extends RuntimeException {

	public AuthenticationException(String message) {
		super(message);
	}
}

//	EXC-005 Invalid login credentials
//	EXC-006 Inactive user login
//	EXC-013 Invalid JWT token
//	EXC-014 Expired JWT token