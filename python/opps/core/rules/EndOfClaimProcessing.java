package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.api.v2.OppsPaymentData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import java.math.BigDecimal;

public class EndOfClaimProcessing
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  /**
   * END OF CLAIM PROCESSING
   *
   * <pre>
   * 1. MOVE TOTAL CLAIM CHARGE AMOUNT.
   * 2. MOVE TOTAL CLAIM PAYMENT AMOUNT.
   * 3. MOVE TOTAL CLAIM BLOOD PINTS USED.
   * 4. CALCULATE CLAIM LEVEL OUTLIER AMOUNT.
   * </pre>
   *
   * <p>(19900-END-PRICE-RTN)
   */
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final OppsPaymentData paymentData = calculationContext.getPaymentData();

    paymentData.setTotalClaimCharges(calculationContext.getTotalCharge());
    paymentData.setTotalPayment(calculationContext.getTotalClaimPayment());

    // NUMBER OF BLOOD PINTS USED FOR BLOOD DEDUCTIBLES ON CLAIM =
    //   INITIAL NUMBER OF BLOOD PINTS ALLOWED FOR DEDUCTIBLES -
    //   BLOOD PINTS STILL LEFT AFTER PROCESSING BLOOD DEDUCTIBLES
    // COMPUTE A-BLOOD-PINTS-USED = H-BENE-BLOOD-PINTS - H-BENE-PINTS-USED
    final int beneficiaryBloodPintsRemaining =
        calculationContext.getClaimData().getBloodPintsRemaining();
    final int beneficiaryBloodPintsUsed = calculationContext.getBeneficiaryBloodPintsUsed();
    paymentData.setBloodPintsUsed(beneficiaryBloodPintsRemaining - beneficiaryBloodPintsUsed);

    final BigDecimal outlierPayment =
        calculationContext.getOutlierPaymentInfo().getOutlierPayment();
    if (BigDecimalUtils.isGreaterThanZero(outlierPayment)) {
      paymentData.setTotalClaimOutlierPayment(outlierPayment);
    }
  }
}
