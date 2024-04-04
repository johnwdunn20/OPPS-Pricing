package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculateDeviceCredit extends AbstractDeductibleLineRule {
  /**
   * CALCULATE THE LINE'S DEVICE CREDIT AMOUNT.
   *
   * <p>(19550-DEVICE-CREDIT)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final LineCalculation lineCalculation = calculationContext.getLineCalculation();
    final BigDecimal claimDeviceCreditAmount = pricerContext.getClaimDeviceCreditAmount();
    final BigDecimal totalDeviceCreditPayments = pricerContext.getTotalDeviceCreditPayments();
    // PKG-BLD-DED-LINE-FLAG is determined in method calculateDeductibleLine and passed in as a
    // parameter
    // BLD-DEDUC-HCPCS-FLAG has been replaced with method isBloodHcpcsDeductible

    // CALCULATE DEVICE CREDIT FOR ELIGIBLE LINE(S)
    if (PaymentAdjustmentFlag.DEVICE_CREDIT_17.is(
        lineCalculation.getLineInput().getPaymentAdjustmentFlags())) {
      if (BigDecimalUtils.isLessThanOrEqualToZero(claimDeviceCreditAmount)
          || BigDecimalUtils.isLessThanOrEqualToZero(totalDeviceCreditPayments)) {
        return;
      }

      final DeductibleLine deductibleLine = lineCalculation.getDeductibleLine();

      // COMPUTE H-LINE-DEVCR-PYMT-RATE ROUNDED = W-APC-PYMT (W-LP-INDX) / H-TOT-DEVCR-PYMTS
      final BigDecimal deviceCreditPaymentRate =
          deductibleLine.getApcPayment().divide(totalDeviceCreditPayments, 4, RoundingMode.HALF_UP);

      // COMPUTE H-LINE-DEVCR-AMT ROUNDED = H-CLAIM-DEVCR-AMT * H-LINE-DEVCR-PYMT-RATE
      lineCalculation.setDeviceCreditAmount(
          claimDeviceCreditAmount
              .multiply(deviceCreditPaymentRate)
              .setScale(2, RoundingMode.HALF_UP));
    }
  }
}
