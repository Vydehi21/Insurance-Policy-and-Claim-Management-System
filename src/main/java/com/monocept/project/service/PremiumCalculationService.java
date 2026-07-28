package com.monocept.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.monocept.project.enums.PremiumType;
import com.monocept.project.exception.BusinessRuleException;
import com.monocept.project.model.PolicyPlan;

/**
 * Single source of truth for premium calculation — used by plan-form live
 * preview (indirectly, via the mirrored frontend formula), the customer
 * quote endpoint, and actual policy purchase/issuance. The backend is
 * always authoritative; a client-supplied premium is never trusted.
 *
 * Formula:
 *   units          = coverageAmount / 50000
 *   annualPremium  = units * plan.ratePerUnit
 *
 *   MONTHLY   -> round(annualPremium / 12)
 *   QUARTERLY -> round(annualPremium / 4)
 *   ANNUAL    -> round(annualPremium * (1 - plan.annualDiscountPercent/100))
 *   ONE_TIME  -> round(annualPremium * plan.duration * (1 - plan.oneTimeDiscountPercent/100))
 *
 * Rounded with HALF_UP to the nearest rupee at the end of the calculation.
 */
@Service
public class PremiumCalculationService {

	private static final BigDecimal FIFTY_THOUSAND = BigDecimal.valueOf(50_000);
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	/**
	 * Full breakdown (annual premium, discount applied, final amount) — use
	 * this for anything customer-facing (quote endpoint, purchase flow) so
	 * the discount can be shown, not just the final number.
	 */
	public record PremiumQuote(
			BigDecimal annualPremium,
			BigDecimal discountPercent,
			BigDecimal discountAmount,
			BigDecimal finalPremium) {
	}

	public PremiumQuote calculateQuote(PolicyPlan plan, BigDecimal coverageAmount, PremiumType premiumType) {
		validateInputs(plan, coverageAmount, premiumType);
		validateCoverageWithinPlanBounds(plan, coverageAmount);

		BigDecimal units = coverageAmount.divide(FIFTY_THOUSAND, 10, RoundingMode.HALF_UP);
		BigDecimal annualPremium = units.multiply(plan.getRatePerUnit()).setScale(0, RoundingMode.HALF_UP);

		BigDecimal discountPercent = discountPercentFor(plan, premiumType);
		BigDecimal rawAmount = rawAmountFor(annualPremium, plan, premiumType);
		BigDecimal discountAmount = rawAmount.multiply(discountPercent)
				.divide(HUNDRED, 10, RoundingMode.HALF_UP)
				.setScale(0, RoundingMode.HALF_UP);

		BigDecimal finalPremium = rawAmount.subtract(discountAmount).setScale(0, RoundingMode.HALF_UP);

		return new PremiumQuote(annualPremium, discountPercent, discountAmount, finalPremium);
	}

	/** Convenience wrapper for call sites that only need the final number. */
	public BigDecimal calculatePremium(PolicyPlan plan, BigDecimal coverageAmount, PremiumType premiumType) {
		return calculateQuote(plan, coverageAmount, premiumType).finalPremium();
	}

	private BigDecimal rawAmountFor(BigDecimal annualPremium, PolicyPlan plan, PremiumType premiumType) {
		return switch (premiumType) {
			case MONTHLY -> annualPremium.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
			case QUARTERLY -> annualPremium.divide(BigDecimal.valueOf(4), 10, RoundingMode.HALF_UP);
			case ANNUAL -> annualPremium;
			case ONE_TIME -> annualPremium.multiply(BigDecimal.valueOf(plan.getDuration()));
		};
	}

	private BigDecimal discountPercentFor(PolicyPlan plan, PremiumType premiumType) {
		return switch (premiumType) {
			case ANNUAL -> nullToZero(plan.getAnnualDiscountPercent());
			case ONE_TIME -> nullToZero(plan.getOneTimeDiscountPercent());
			case MONTHLY, QUARTERLY -> BigDecimal.ZERO;
		};
	}

	private BigDecimal nullToZero(BigDecimal value) {
		return value != null ? value : BigDecimal.ZERO;
	}

	private void validateInputs(PolicyPlan plan, BigDecimal coverageAmount, PremiumType premiumType) {
		if (plan == null || coverageAmount == null || premiumType == null) {
			throw new IllegalArgumentException(
					"Plan, coverage amount, and premium type are all required to calculate a premium");
		}
	}

	/**
	 * Defense in depth — re-checked here even though callers should have
	 * already validated coverage against the plan's bounds.
	 */
	private void validateCoverageWithinPlanBounds(PolicyPlan plan, BigDecimal coverageAmount) {
		BigDecimal min = plan.getMinCoverageAmount();
		BigDecimal max = plan.getMaxCoverageAmount();

		if (coverageAmount.compareTo(min) < 0 || coverageAmount.compareTo(max) > 0) {
			throw new BusinessRuleException(
					"Coverage amount must be between " + min + " and " + max + " for this plan");
		}
	}
}