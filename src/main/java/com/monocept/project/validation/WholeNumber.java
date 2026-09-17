package com.monocept.project.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Rejects any BigDecimal amount with a non-zero fractional part.
 * Applies to money fields (coverage/premium amounts) that must always be
 * whole rupees — no paise, no floating point.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = WholeNumberValidator.class)
public @interface WholeNumber {
	String message() default "must be a whole number (decimal values are not allowed)";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}