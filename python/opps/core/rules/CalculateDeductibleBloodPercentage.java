package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import java.math.RoundingMode;

public class CalculateDeductibleBloodPercentage
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  /**
   * CALCULATE RATIO OF BLOOD PRODUCT CHARGES TO TOTAL BLOOD CHARGES (PRODUCT &amp;
   * STORAGE/PROCESSING).
   *
   * <p>(Extracted from 19000-PROCESS-MAIN-NEW)
   */
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    if (BigDecimalUtils.isGreaterThanZero(calculationContext.getBloodProductCharges())) {
      // COMPUTE H-38X-39X-RATE ROUNDED = H-TOT-38X / H-TOT-38X-39X
      calculationContext.setBloodProductPercentage(
          calculationContext
              .getBloodProductCharges()
              .divide(calculationContext.getTotalBloodCharges(), 4, RoundingMode.HALF_UP));
    }
  }
}
