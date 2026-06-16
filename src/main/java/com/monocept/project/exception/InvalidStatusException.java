 package com.monocept.project.exception;



public class InvalidStatusException extends RuntimeException{



    public InvalidStatusException(String message){

        super(message);

    }

}



//	EXC-009 Invalid policy status

//	EXC-010 Invalid claim status

//	EXC-016 Modify approved/rejected claim