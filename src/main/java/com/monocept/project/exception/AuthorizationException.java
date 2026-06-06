package com.monocept.project.exception;

public class AuthorizationException extends RuntimeException{
	
    public AuthorizationException(String message){
        super(message);
    }
}

//	EXC-007 Unauthorized access
//	EXC-008 Forbidden role access
//	EXC-015 Accessing another customer's data