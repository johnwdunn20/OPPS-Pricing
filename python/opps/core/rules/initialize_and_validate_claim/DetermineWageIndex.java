package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.EvaluatingCalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import java.util.List;

/** Rule set for determining the wage index if not previously set. */
public class DetermineWageIndex
    extends EvaluatingCalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  public DetermineWageIndex(
      List<CalculationRule<OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext>>
          rules) {
    super(rules);
  }

  /**
   * These rules only apply if the wage index was not previously set (CBSA is present and wage index
   * is zero).
   */
  @Override
  public boolean shouldExecute(OppsPricerContext calculationContext) {
    // Do not override wage index if previously set (see DetermineCbsa - SpecialPaymentIndicator)
    return null != calculationContext.getPaymentData().getFinalCbsa()
        && BigDecimalUtils.isZero(calculationContext.getWageIndex());
  }
}
