package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class WageIndexTransitionAdjustment
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  /**
   * ALL PROVIDERS ELIGIBLE FOR TRANSITION DUE TO CHANGE OF RURAL FLOOR POLICY: USING RURAL WAGE
   * INDEX.
   *
   * <pre>
   *       - COMPARE CURRENT CY TO PREVIOUS CY WAGE INDEX
   *         IF WAGE INDEX GOES DOWN BY MORE THAN 5% BETWEEN
   *         CURRENT CY AND PREVIOUS CY ASSIGN PREVIOUS CY
   *         WAGE INDEX WITH A CAP OF THE 5% REDUCTION
   *       - IF PROVIDER SPECIAL WAGE INDEX OR SUPPLEMENTAL WAGE INDEX
   *         NOT POPULATED, PRICER WILL SKIP CAPPING LOGIC AND USE THE 2021 CBSA
   *         WAGE INDEX TO PRICE CLAIM
   * </pre>
   *
   * <p>(19122-WI-TRANSITION-ADJ)
   */
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final String providerNumber = calculationContext.getProviderData().getProviderCcn();
    final BigDecimal wageIndex = calculationContext.getWageIndex();

    final BigDecimal previousWageIndex =
        calculationContext.getDataTables().getPriorYearWageIndex(providerNumber);

    if (previousWageIndex == null) {
      calculationContext.applyClaimReturnCode(ReturnCode.WAGE_INDEX_NOT_FOUND_50);
      return;
    }

    if (BigDecimalUtils.isZero(previousWageIndex)) {
      return;
    }

    if (BigDecimalUtils.isGreaterThan(
        calculationContext.getWageIndexPercentReduction(),
        wageIndex.subtract(previousWageIndex).divide(previousWageIndex, RoundingMode.HALF_UP))) {

      // COMPUTE H-WINX ROUNDED = H-PREV-WINX * WI-PCT-ADJ-CY2020
      calculationContext.setWageIndex(
          previousWageIndex
              .multiply(calculationContext.getWageIndexPercentAdjustment())
              .setScale(4, RoundingMode.HALF_UP));
    }
  }
}
