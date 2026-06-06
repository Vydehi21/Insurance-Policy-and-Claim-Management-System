package com.monocept.project.exception;

public class ResourceNotFoundException extends RuntimeException {
	public ResourceNotFoundException(String message) {
		super(message);
	}
}

//	EXC-001 Resource not found
