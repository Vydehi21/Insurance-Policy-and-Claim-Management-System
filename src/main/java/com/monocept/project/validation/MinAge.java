package com.monocept.project.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Rejects a LocalDate of birth that indicates the person is younger than
 * {@link #value()} years old as of today. Works alongside {@code @Past}
 * (which only rejects future dates) so that an under-age date of birth is
 * rejected server-side too, not only by the client's date picker.
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinAgeValidator.class)
public @interface MinAge {
	int value() default 18;
	String message() default "Date of birth indicates age must be at least 18 years";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}