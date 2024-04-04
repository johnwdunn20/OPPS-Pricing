package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculatePassThroughDeviceOffsets
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    // CALCULATE WAGE-ADJUSTED PASS-THROUGH DEVICE OFFSETS (VALUE CODES QN & QO; CLAIM LEVEL)
    calculationContext.setQnWageAdjustedPassThroughDeviceOffset(
        calculateWageAdjustedOffset(
            BigDecimalUtils.defaultValue(
                calculationContext
                    .getClaimData()
                    .getPayerOnlyValueCodeOffsets()
                    .getApcDeviceOffsetAmountQn()),
            calculationContext.getWageIndex()));

    calculationContext.setQoWageAdjustedPassThroughDeviceOffset(
        calculateWageAdjustedOffset(
            BigDecimalUtils.defaultValue(
                calculationContext
                    .getClaimData()
                    .getPayerOnlyValueCodeOffsets()
                    .getApcDeviceOffsetAmountQo()),
            calculationContext.getWageIndex()));
  }

  /**
   * CALCULATE WAGE-ADJUSTED PASS-THROUGH DEVICE OFFSET.
   *
   * <p>(Extracted from 19000-PROCESS-MAIN-NEW)
   *
   * @param valueCodeAmount Value Code
   * @return 60% of value code times the wage index plus the remaining 40% of the value code.
   */
  protected BigDecimal calculateWageAdjustedOffset(
      BigDecimal valueCodeAmount, BigDecimal wageIndex) {

    // COMPUTE H-QN-WA-PTD-OFFSET ROUNDED =
    //                        ((L-PAYER-ONLY-VC-QN * .60) * H-WINX) +
    //                        (L-PAYER-ONLY-VC-QN * .40)

    // COMPUTE H-QO-WA-PTD-OFFSET ROUNDED =
    //                        ((L-PAYER-ONLY-VC-QO * .60) * H-WINX) +
    //                        (L-PAYER-ONLY-VC-QO * .40)

    return valueCodeAmount
        .multiply(new BigDecimal(".60"))
        .multiply(wageIndex)
        .add(valueCodeAmount.multiply(new BigDecimal(".40")))
        .setScale(2, RoundingMode.HALF_UP);
  }
}
