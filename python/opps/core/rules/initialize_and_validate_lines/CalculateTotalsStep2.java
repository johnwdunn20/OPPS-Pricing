package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import java.math.BigDecimal;
import java.util.List;

public class CalculateTotalsStep2 extends AbstractLineCalculationRule {

  /** (Extracted from 19150-INIT). */
  @Override
  public void calculate(ServiceLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final IoceServiceLineData ioceServiceLine = calculationContext.getInput();
    final BigDecimal submittedCharge = ioceServiceLine.getCoveredCharges();
    final List<String> paymentAdjustmentFlags = ioceServiceLine.getPaymentAdjustmentFlags();

    // PROCESS BLOOD DEDUCTIBLE (EFFECTIVE AS OF 07-01-2005)
    // - TOTAL BLOOD CODE CHARGES WHEN PAF = '5' OR '6'
    //   5: BLOOD/BLOOD PRODUCT USED IN BLOOD DEDUC CALC
    //   6: BLOOD PROCESS/STORAGE (LABOR) NOT SBJ TO BLD DEDUC
    if (OppsPricerContext.isBloodPaymentAdjustment(paymentAdjustmentFlags)) {
      if (PaymentAdjustmentFlag.BLOOD_DEDUCTIBLE_5.is(paymentAdjustmentFlags)) {

        // COMPUTE H-TOT-38X = OPPS-SUB-CHRG (LN-SUB) + H-TOT-38X
        pricerContext.setBloodProductCharges(
            pricerContext.getBloodProductCharges().add(submittedCharge));
      }
      // COMPUTE H-TOT-38X-39X = OPPS-SUB-CHRG (LN-SUB) + H-TOT-38X-39X
      pricerContext.setTotalBloodCharges(pricerContext.getTotalBloodCharges().add(submittedCharge));
    }

    // ACCUMULATE APC PAYMENTS OF LINES ELIGIBLE FOR DEVICE CREDIT
    // WHEN THE CLAIM'S TOTAL DEVICE CREDIT IS > $0 AND THE LINE
    // HAS PAYMENT ADJUSTMENT FLAG '17'
    if (BigDecimalUtils.isGreaterThanZero(pricerContext.getClaimDeviceCreditAmount())
        && PaymentAdjustmentFlag.DEVICE_CREDIT_17.is(paymentAdjustmentFlags)) {

      // COMPUTE H-TOT-DEVCR-PYMTS = H-TOT-DEVCR-PYMTS + H-APC-PYMT
      pricerContext.setTotalDeviceCreditPayments(
          pricerContext.getTotalDeviceCreditPayments().add(pricerContext.getApcPayment()));
    }

    // ACCUMULATE APC PAYMENTS OF TERMINATED PROCEDURE LINES
    // ELIGIBLE FOR DEVICE OFFSET WHEN PAYER ONLY VALUE CODE QQ
    // IS > $0 AND THE LINE HAS PAYMENT ADJUSTMENT FLAG '16'
    // (LINE MODIFIER IS 73)
    if (BigDecimalUtils.isGreaterThanZero(
        BigDecimalUtils.defaultValue(
            pricerContext
                .getClaimData()
                .getPayerOnlyValueCodeOffsets()
                .getTerminatedPassthroughOffsetAmountQq()))) {

      // COMPUTE H-TOT-TPDO-PYMTS = H-TOT-TPDO-PYMTS + H-APC-PYMT
      pricerContext.setTotalTerminatedProcedureDeviceOffsetPayments(
          pricerContext
              .getTotalTerminatedProcedureDeviceOffsetPayments()
              .add(pricerContext.getApcPayment()));
    }

    // ACCUMULATE LINE CHARGES OF PASS-THROUGH DEVICE LINES
    // ASSOCIATED WITH PAYER ONLY VALUE CODES QN AND QO
    // - LINES WITH PAF '12' RECEIVE OFFSET IN VALUE CODE QN
    if (PaymentAdjustmentFlag.DEVICE_PASS_THROUGH_12.is(paymentAdjustmentFlags)) {
      // COMPUTE H-QN-TOT-PTD-CHARGES = H-QN-TOT-PTD-CHARGES + OPPS-SUB-CHRG (LN-SUB)
      pricerContext.setTotalQnPassThroughDeviceCharges(
          pricerContext.getTotalQnPassThroughDeviceCharges().add(submittedCharge));
    }

    // - LINES WITH PAF '13' RECEIVE OFFSET IN VALUE CODE QO
    if (PaymentAdjustmentFlag.DEVICE_PASS_THROUGH_13.is(paymentAdjustmentFlags)) {
      // COMPUTE H-QO-TOT-PTD-CHARGES = H-QO-TOT-PTD-CHARGES + OPPS-SUB-CHRG (LN-SUB)
      pricerContext.setTotalQoPassThroughDeviceCharges(
          pricerContext.getTotalQoPassThroughDeviceCharges().add(submittedCharge));
    }
  }
}
