package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;

public class WageIndexCheck
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  @Override
  public void calculate(OppsPricerContext calculationContext) {
    // SET ERROR CODE IF THE WAGE INDEX = 0
    if (BigDecimalUtils.isZero(calculationContext.getWageIndex())
        && Integer.parseInt(calculationContext.getOutput().getReturnCodeData().getCode()) == 1) {
      calculationContext.applyClaimReturnCode(ReturnCode.WAGE_INDEX_EQUALS_ZERO_51);
      return;
    }

    // MOVE WAGE INDEX TO VARIABLE TO BE PASSED BACK
    calculationContext.getPaymentData().setFinalWageIndex(calculationContext.getWageIndex());
  }
}
