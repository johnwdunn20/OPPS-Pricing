package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.OppsPaymentData;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;

public class AccumulateClaimTotals
    implements CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  /** Rule only applies to lines without an error code. */
  @Override
  public boolean shouldExecute(DeductibleLineContext calculationContext) {
    final ServiceLinePaymentData lineOutput = calculationContext.getOutput();
    return Integer.parseInt(lineOutput.getReturnCode().getCode()) < 30;
  }

  /**
   * ACCUMULATE CLAIM TOTALS USING DATA FROM THE CURRENT VALID SERVICE LINE (IN THE COINSURANCE
   * DEDUCTIBLE TABLE).
   *
   * <p>(Extracted from 19400-CALCULATE)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();

    final LineCalculation lineCalculation = calculationContext.getLineCalculation();
    final OppsPaymentData paymentData = pricerContext.getOutput().getPaymentData();
    final String lineActionFlag = calculationContext.getInput().getServiceLine().getActionFlag();

    // COMPUTE A-TOTAL-CLM-DEDUCT = H-TOTAL-LN-DEDUCT + A-TOTAL-CLM-DEDUCT
    paymentData.setTotalClaimDeductible(
        lineCalculation.getTotalDeductible().add(paymentData.getTotalClaimDeductible()));

    // Line payments with an external line item adjustment are processed later
    if (!OppsPricerContext.hasExternalAdjustment(lineActionFlag)) {
      // COMPUTE H-TOT-PYMT = H-TOT-PYMT + H-LITEM-PYMT
      pricerContext.setTotalClaimPayment(
          pricerContext.getTotalClaimPayment().add(lineCalculation.getPayment()));
    }

    // COMPUTE A-BLOOD-DEDUCT-DUE = A-BLOOD-DEDUCT-DUE + H-LN-BLOOD-DEDUCT
    paymentData.setBloodDeductibleDue(
        paymentData.getBloodDeductibleDue().add(lineCalculation.getBloodDeductible()));

    // COMPUTE A-DEVICE-CREDIT-QD = A-DEVICE-CREDIT-QD + H-LINE-DEVCR-AMT
    paymentData.setDeviceCreditQd(
        paymentData.getDeviceCreditQd().add(lineCalculation.getDeviceCreditAmount()));
  }
}
