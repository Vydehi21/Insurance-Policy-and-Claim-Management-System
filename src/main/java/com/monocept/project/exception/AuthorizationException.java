package com.monocept.project.exception;

public class AuthorizationException extends RuntimeException{
	
    public AuthorizationException(String message){
        super(message);
    }
}
