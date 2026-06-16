package com.monocept.project.exception;

public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message){
        super(message);
    }
}