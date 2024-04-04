package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class WageIndexTransitionAdjustment2021
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  // bypass this rule when supplemental wage index indicator not equal to '1'
  @Override
  public boolean shouldExecute(OppsPricerContext calculationContext) {
    return "1".equals(calculationContext.getProviderData().getSupplementalWageIndexIndicator());
  }

  @Override
  public void calculate(OppsPricerContext calculationContext) {
    // Assign local variable for supplemental wage index,
    // set return code 50 if less than or equal to 0
    final BigDecimal supplementalWageIndex =
        calculationContext.getProviderData().getSupplementalWageIndex();
    if (BigDecimalUtils.isLessThanOrEqualToZero(supplementalWageIndex)) {
      calculationContext.applyClaimReturnCode(ReturnCode.WAGE_INDEX_NOT_FOUND_50);
      return;
    }

    // Validation check for provider record effective date within calendar year
    // set return code '50' if not
    final LocalDate effectiveDate = calculationContext.getProviderData().getEffectiveDate();
    final LocalDate serviceDate = calculationContext.getClaimData().getServiceFromDate();

    if (effectiveDate.getYear() != serviceDate.getYear()) {
      calculationContext.applyClaimReturnCode(ReturnCode.WAGE_INDEX_NOT_FOUND_50);
      return;
    }

    // ALL PROVIDERS ELIGIBLE FOR TRANSITION DUE TO CHANGE
    // OF RURAL FLOOR POLICY: USING RURAL WAGE INDEX
    calculationContext.setWageIndex(applyWageIndexTransitionAdjustment(calculationContext));
  }

  /**
   * ALL PROVIDERS ELIGIBLE FOR TRANSITION DUE TO CHANGE OF RURAL FLOOR POLICY: USING RURAL WAGE
   * INDEX.
   *
   * <pre>
   *       - COMPARE CURRENT CY TO SUPPLEMENTAL WAGE INDEX
   *         IF WAGE INDEX GOES DOWN BY MORE THAN 5% BETWEEN
   *         CURRENT CY AND SUPPLEMENTAL WI ASSIGN SUPPLEMENTAL
   *         WAGE INDEX WITH A CAP OF THE 5% REDUCTION
   *       - IF PROVIDER SPECIAL WAGE INDEX OR SUPPLEMENTAL WAGE
   *         INDEX NOT POPULATED, PRICER WILL SKIP CAPPING LOGIC
   *         AND USE THE 2021 CBSA WAGE INDEX TO PRICE CLAIM
   *       - ADDED VALIDATION FOR SUPPLEMENTAL WAGE INDEX AND
   *         INDICATOR WITH RETURN CODE 50
   * </pre>
   *
   * <p>(20122-WI-TRANSITION-ADJ)
   */
  private BigDecimal applyWageIndexTransitionAdjustment(OppsPricerContext calculationContext) {
    final BigDecimal supplementalWageIndex =
        calculationContext.getProviderData().getSupplementalWageIndex();
    final BigDecimal wageIndex = calculationContext.getWageIndex();

    if (BigDecimalUtils.isLessThan(
        wageIndex
            .subtract(supplementalWageIndex)
            .divide(supplementalWageIndex, RoundingMode.HALF_UP),
        calculationContext.getWageIndexPercentReduction())) {

      // COMPUTE H-WINX ROUNDED = supplementalWageIndex * WI-PCT-ADJ-CY2020
      return supplementalWageIndex
          .multiply(calculationContext.getWageIndexPercentAdjustment())
          .setScale(4, RoundingMode.HALF_UP);
    }

    return wageIndex;
  }
}
