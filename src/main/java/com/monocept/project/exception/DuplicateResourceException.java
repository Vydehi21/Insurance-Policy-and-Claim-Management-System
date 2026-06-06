package com.monocept.project.exception;

public class DuplicateResourceException extends RuntimeException {

	public DuplicateResourceException(String message) {
		super(message);
	}

}

//	EXC-002 Duplicate email
//	EXC-003 Duplicate product name
//	EXC-004 Duplicate transaction reference
