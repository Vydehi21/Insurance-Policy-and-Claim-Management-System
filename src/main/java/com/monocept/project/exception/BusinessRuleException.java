package com.monocept.project.exception;

public class BusinessRuleException extends RuntimeException{

    public BusinessRuleException(String message){
        super(message);
    }
}
//	EXC-011 Claim amount exceeding coverage