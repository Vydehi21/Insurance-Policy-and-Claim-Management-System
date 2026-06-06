package com.monocept.project.exception;

public class InvalidRequestException extends RuntimeException {

	public InvalidRequestException(String message) {
		super(message);
	}

}

//	EXC-017 Invalid pagination request
//	EXC-018 Invalid sorting field