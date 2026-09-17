package com.monocept.project.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * PLN-BR / PAYBR: coverage and premium amounts must be in multiples of
 * ₹50,000.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MultipleOf50000Validator.class)
public @interface MultipleOf50000 {
	String message() default "must be in multiples of 50,000";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}