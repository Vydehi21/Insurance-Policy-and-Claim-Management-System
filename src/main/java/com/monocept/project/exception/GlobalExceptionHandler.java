package com.monocept.project.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
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

import com.monocept.project.dto.ErrorResponseDTO;
import com.monocept.project.dto.ValidationErrorResponseDTO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDTO> handleDuplicateResponse(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.warn("Duplicate resource: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(InvalidJwtTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidJwt(
            InvalidJwtTokenException ex,
            HttpServletRequest request) {

        log.warn("Invalid JWT token: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(ExpiredJwtTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleExpiredJwt(
            ExpiredJwtTokenException ex,
            HttpServletRequest request) {

        log.warn("Expired JWT token: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthorization(
            AuthorizationException ex,
            HttpServletRequest request) {

        log.warn("Authorization failed: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidStatus(
            InvalidStatusException ex,
            HttpServletRequest request) {

        log.warn("Invalid status: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRule(
            BusinessRuleException ex,
            HttpServletRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidRequest(
            InvalidRequestException ex,
            HttpServletRequest request) {

        log.warn("Invalid request: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation failed");

        Map<String, String> validationErrors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        ValidationErrorResponseDTO response =
                new ValidationErrorResponseDTO(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        validationErrors,
                        request.getRequestURI()
                );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        log.warn("Invalid parameter: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid input. Please provide valid data.",
                request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("Invalid JSON request body");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid JSON request body.",
                request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("Database constraint violation occured ");

        return buildResponse(
                HttpStatus.CONFLICT,
                "Duplicate or invalid database value.",
                request.getRequestURI());
    }

 

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error occurred", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong.",
                request.getRequestURI());
    }
    


    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {
    	
    	 log.warn(
    	            "Security failure. Access denied for request: {}",
    	            request.getRequestURI());

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Access denied",
                request.getRequestURI());
    }
    private ResponseEntity<ErrorResponseDTO> buildResponse(
            HttpStatus status,
            String message,
            String path) {

        ErrorResponseDTO error = new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path);

        return new ResponseEntity<>(error, status);
    }
    
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidOperation(
            InvalidOperationException ex,
            HttpServletRequest request){

        log.warn(
        "Invalid operation: {}",
        ex.getMessage());

        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI());
    }
}