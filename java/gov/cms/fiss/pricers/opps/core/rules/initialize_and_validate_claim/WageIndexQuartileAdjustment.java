package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class WageIndexQuartileAdjustment
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  /**
   * PROVIDERS BELOW WAGE INDEX OF WAGE-INDEX-QUARTILE WILL RECEIVE A BOOST IN THEIR WAGE INDEX.
   *
   * <pre>
   *   THIS WILL BE EFF. FOR 4 YEARS (CY2020 - CY2023)
   *       - THE STATIC WAGE-INDEX-QUARTILE WILL CHANGE EVERY YEAR
   * </pre>
   *
   * <p>(19121-WI-QUARTILE-ADJ)
   */
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final BigDecimal wageIndex = calculationContext.getWageIndex();
    final BigDecimal quartileWageIndex = calculationContext.getWageIndexQuartile();

    // PROVIDERS BELOW WAGE INDEX OF WAGE INDEX QUARTILE WILL RECEIVE A BOOST IN THEIR WAGE INDEX.
    if (BigDecimalUtils.isGreaterThan(quartileWageIndex, wageIndex)) {

      // COMPUTE H-WINX ROUNDED = ((WI-QUARTILE-CY2020 - H-WINX) / 2) + H-WINX
      final BigDecimal adjustedWageIndex =
          quartileWageIndex
              .subtract(wageIndex)
              .divide(new BigDecimal("2.00"), RoundingMode.HALF_UP)
              .add(wageIndex);

      calculationContext.setWageIndex(adjustedWageIndex);
    }
  }
}
