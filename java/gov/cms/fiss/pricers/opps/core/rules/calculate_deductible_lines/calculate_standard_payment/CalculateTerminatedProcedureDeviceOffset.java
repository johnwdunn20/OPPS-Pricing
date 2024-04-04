package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculateTerminatedProcedureDeviceOffset extends AbstractDeductibleLineRule {
  /**
   * CALCULATE THE TERMINATED PROCEDURE LINE'S DEVICE OFFSET.
   *
   * <p>(19550-TERM-PROC-DEV-OFF)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final OppsClaimData claimData = pricerContext.getClaimData();
    final BigDecimal payerValueCodeQq =
        BigDecimalUtils.defaultValue(
            claimData.getPayerOnlyValueCodeOffsets().getTerminatedPassthroughOffsetAmountQq());
    final DeductibleLine deductibleLine = calculationContext.getInput();
    final BigDecimal totalTerminatedProcedureDeviceOffsetPayments =
        pricerContext.getTotalTerminatedProcedureDeviceOffsetPayments();
    // CALCULATE DEVICE OFFSET FOR ELIGIBLE TERMINATED PROCEDURE LINES AND
    // REDUCE THE APC PAYMENT BY THE DEVICE OFFSET AMOUNT
    if (PaymentAdjustmentFlag.TERMINATED_PROCEDURE_PASS_THROUGH_DEVICE_16.is(
        deductibleLine.getServiceLine().getPaymentAdjustmentFlags())) {
      if (BigDecimalUtils.isLessThanOrEqualToZero(payerValueCodeQq)
          && BigDecimalUtils.isLessThanOrEqualToZero(
              totalTerminatedProcedureDeviceOffsetPayments)) {
        return;
      }

      final BigDecimal lineApcPayment = deductibleLine.getApcPayment();

      // CALCULATE THE LINE'S PORTION OF THE CLAIM DEVICE OFFSET FOR TERMINATED PROCEDURES
      // COMPUTE H-LINE-TPDO-PYMT-RATE ROUNDED = W-APC-PYMT (W-LP-INDX) / H-TOT-TPDO-PYMTS
      final BigDecimal rate =
          lineApcPayment.divide(
              totalTerminatedProcedureDeviceOffsetPayments, 4, RoundingMode.HALF_UP);

      // COMPUTE H-LINE-TPDO-AMT ROUNDED = L-PAYER-ONLY-VC-QQ * H-LINE-TPDO-PYMT-RATE
      final BigDecimal offset = rate.multiply(payerValueCodeQq).setScale(2, RoundingMode.HALF_UP);

      // ADJUST THE BASE APC PAYMENT BY THE DEVICE OFFSET
      if (BigDecimalUtils.isGreaterThanOrEqualTo(lineApcPayment, offset)) {

        // COMPUTE W-APC-PYMT (W-LP-INDX) ROUNDED = W-APC-PYMT (W-LP-INDX) - H-LINE-TPDO-AMT
        deductibleLine.setApcPayment(lineApcPayment.subtract(offset));
      } else {
        deductibleLine.setApcPayment(BigDecimalUtils.ZERO);
      }
    }
  }
}
