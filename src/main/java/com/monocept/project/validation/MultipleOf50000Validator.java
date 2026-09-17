package com.monocept.project.validation;

import java.math.BigDecimal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MultipleOf50000Validator implements ConstraintValidator<MultipleOf50000, BigDecimal> {

	private static final BigDecimal UNIT = BigDecimal.valueOf(50_000);

	@Override
	public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		return value.remainder(UNIT).compareTo(BigDecimal.ZERO) == 0;
	}
}