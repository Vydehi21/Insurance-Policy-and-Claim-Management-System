package com.monocept.project.validation;

import java.math.BigDecimal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class WholeNumberValidator implements ConstraintValidator<WholeNumber, BigDecimal> {

	@Override
	public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
		// Let @NotNull handle nullness; this validator only checks shape.
		if (value == null) {
			return true;
		}
		return value.stripTrailingZeros().scale() <= 0;
	}
}