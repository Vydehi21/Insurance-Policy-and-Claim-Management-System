package com.monocept.project.exception;

import java.nio.file.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.monocept.project.config.AppConfig;
import com.monocept.project.repository.ClaimDocumentRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	
	private final ClaimDocumentRepository claimDocumentRepository;
	private final AppConfig appConfig;
	GlobalExceptionHandler(AppConfig appConfig, ClaimDocumentRepository claimDocumentRepository) {
		this.appConfig = appConfig;
		this.claimDocumentRepository = claimDocumentRepository;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex){
		log.warn("Resource not found: {}", ex.getMessage());
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}
	
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<Map<String, Object>> handleDuplicateResponse(DuplicateResourceException ex){
		log.warn("Duplicate resource: {}", ex.getMessage());
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
	}
	
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex){
		log.warn("Authentication failed: {}", ex.getMessage());
		return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}
	
	@ExceptionHandler(AuthorizationException.class)
	public ResponseEntity<Map<String, Object>> handleAuthtorization(AuthorizationException ex){
		log.warn("Authorization failed: {}", ex.getMessage());
		return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
	}
	
	@ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStatus(InvalidStatusException ex) {
        log.warn("Invalid status: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
	
	@ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
	
	@ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidRequestException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
	
	@ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed");
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Validation Failed");
        error.put("messages", validationErrors);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid path variable or request parameter: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid input. Please provide valid data.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(HttpMessageNotReadableException ex) {
        log.warn("Invalid JSON request body");
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid JSON request body.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Database constraint violation: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Duplicate or invalid database value.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong.");
    }
	
	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message){
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("timestamp", LocalDateTime.now());
		error.put("status", status.value());
		error.put("error", status.getReasonPhrase());
		error.put("message", message);
		
		return new ResponseEntity<>(error, status);
	}
}

//package com.monocept.project.exception;
//
//import java.nio.file.AccessDeniedException;
//import java.time.LocalDateTime;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
//
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(ResourceNotFoundException.class)
//    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
//        log.warn("Resource not found: {}", ex.getMessage());
//        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
//    }
//
//    @ExceptionHandler(DuplicateResourceException.class)
//    public ResponseEntity<Map<String, Object>> handleDuplicateResponse(DuplicateResourceException ex) {
//        log.warn("Duplicate resource: {}", ex.getMessage());
//        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
//    }
//
//    @ExceptionHandler(AuthenticationException.class)
//    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
//        log.warn("Authentication failed: {}", ex.getMessage());
//        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
//    }
//
//    // FIX:
//    // Added dedicated handler for invalid JWT token
//    @ExceptionHandler(InvalidJwtTokenException.class)
//    public ResponseEntity<Map<String, Object>> handleInvalidJwt(InvalidJwtTokenException ex) {
//        log.warn("Invalid JWT token: {}", ex.getMessage());
//        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
//    }
//
//    // FIX:
//    // Added dedicated handler for expired JWT token
//    @ExceptionHandler(ExpiredJwtTokenException.class)
//    public ResponseEntity<Map<String, Object>> handleExpiredJwt(ExpiredJwtTokenException ex) {
//        log.warn("Expired JWT token: {}", ex.getMessage());
//        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
//    }
//
//    @ExceptionHandler(AuthorizationException.class)
//    public ResponseEntity<Map<String, Object>> handleAuthorization(AuthorizationException ex) {
//        log.warn("Authorization failed: {}", ex.getMessage());
//        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
//    }
//
//    @ExceptionHandler(InvalidStatusException.class)
//    public ResponseEntity<Map<String, Object>> handleInvalidStatus(InvalidStatusException ex) {
//        log.warn("Invalid status: {}", ex.getMessage());
//        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
//    }
//
//    @ExceptionHandler(BusinessRuleException.class)
//    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
//        log.warn("Business rule violation: {}", ex.getMessage());
//        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
//    }
//
//    @ExceptionHandler(InvalidRequestException.class)
//    public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidRequestException ex) {
//        log.warn("Invalid request: {}", ex.getMessage());
//        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
//    }
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
//        log.warn("Illegal argument: {}", ex.getMessage());
//        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
//
//        log.warn("Validation failed");
//
//        Map<String, String> validationErrors = new LinkedHashMap<>();
//
//        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
//            validationErrors.put(
//                    fieldError.getField(),
//                    fieldError.getDefaultMessage()
//            );
//        }
//
//        Map<String, Object> error = new LinkedHashMap<>();
//        error.put("timestamp", LocalDateTime.now());
//        error.put("status", HttpStatus.BAD_REQUEST.value());
//        error.put("error", "Validation Failed");
//        error.put("messages", validationErrors);
//
//        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
//        log.warn("Invalid path variable or request parameter: {}", ex.getMessage());
//        return buildResponse(
//                HttpStatus.BAD_REQUEST,
//                "Invalid input. Please provide valid data."
//        );
//    }
//
//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<Map<String, Object>> handleInvalidJson(HttpMessageNotReadableException ex) {
//        log.warn("Invalid JSON request body");
//        return buildResponse(
//                HttpStatus.BAD_REQUEST,
//                "Invalid JSON request body."
//        );
//    }
//
//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
//        log.error("Database constraint violation: {}", ex.getMessage());
//        return buildResponse(
//                HttpStatus.CONFLICT,
//                "Duplicate or invalid database value."
//        );
//    }
//
//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
//        log.warn("Access denied: {}", ex.getMessage());
//        return buildResponse(
//                HttpStatus.FORBIDDEN,
//                "You do not have permission to access this resource."
//        );
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
//        log.error("Unexpected error occurred", ex);
//        return buildResponse(
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                "Something went wrong."
//        );
//    }
//
//    private ResponseEntity<Map<String, Object>> buildResponse(
//            HttpStatus status,
//            String message) {
//
//        Map<String, Object> error = new LinkedHashMap<>();
//
//        error.put("timestamp", LocalDateTime.now());
//        error.put("status", status.value());
//        error.put("error", status.getReasonPhrase());
//        error.put("message", message);
//
//        return new ResponseEntity<>(error, status);
//    }
//}
