package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimData;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import java.math.BigDecimal;

/** DETERMINE THE TOTAL DEVICE CREDIT AMOUNT TO BE DEDUCTED FROM THE CLAIM. */
public class DetermineDeviceCreditAmount
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final OppsClaimData claimData = calculationContext.getClaimData();

    // USE THE LESSER OF THE AMOUNT IN VALUE CODE FD AND THE CAP AMOUNT IN VALUE CODE QU
    final BigDecimal deviceCreditCapOffsetAmountQu =
        BigDecimalUtils.defaultValue(
            claimData.getPayerOnlyValueCodeOffsets().getDeviceCreditCapOffsetAmountQu());

    if (BigDecimalUtils.isGreaterThanZero(deviceCreditCapOffsetAmountQu)
        && BigDecimalUtils.isGreaterThanZero(claimData.getDeviceCredit())) {
      // Set the device credit amount to the lesser of value code and device credit.
      calculationContext.setClaimDeviceCreditAmount(
          deviceCreditCapOffsetAmountQu.min(claimData.getDeviceCredit()));
    }
  }
}
