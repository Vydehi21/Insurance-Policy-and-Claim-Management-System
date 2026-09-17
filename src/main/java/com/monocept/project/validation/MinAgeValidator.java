package com.monocept.project.validation;

import java.time.LocalDate;
import java.time.Period;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinAgeValidator implements ConstraintValidator<MinAge, LocalDate> {

	private int minAge;

	@Override
	public void initialize(MinAge constraintAnnotation) {
		this.minAge = constraintAnnotation.value();
	}

	@Override
	public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
		// Let @NotNull / @Past handle null/future values; this validator only checks age.
		if (dateOfBirth == null || dateOfBirth.isAfter(LocalDate.now())) {
			return true;
		}
		return Period.between(dateOfBirth, LocalDate.now()).getYears() >= minAge;
	}
}