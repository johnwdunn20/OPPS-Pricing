package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimData;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ColonialProcedureCoinsuranceCheck
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  @Override
  public void calculate(OppsPricerContext oppsPricerContext) {
    final OppsClaimData claimData = oppsPricerContext.getClaimData();
    // Check to see if Payment Adjustment Flag '25' has been received by IOCE
    for (final IoceServiceLineData lineInput : claimData.getIoceServiceLines()) {
      if (PaymentAdjustmentFlag.COLONIAL_PROCEDURE_25.is(lineInput.getPaymentAdjustmentFlags())) {
        applyCap(
            oppsPricerContext.getServiceLinePaymentByLineNumber(lineInput.getLineNumber()),
            oppsPricerContext.getColonialProcedureCap());
      }
    }
  }

  /**
   * Apply cap if coinsurance percentage exceeds given cap rate for the year.
   *
   * @param line service output line to adjust
   * @param coinsuranceCap maximum coinsurance rate
   */
  private void applyCap(ServiceLinePaymentData line, BigDecimal coinsuranceCap) {
    final BigDecimal payment = line.getPayment();
    final BigDecimal coinsurance = line.getCoinsuranceAmount();
    final BigDecimal reimbursement = line.getReimbursementAmount();

    // If payment is not greater than ZERO, then exit.
    if (!BigDecimalUtils.isGreaterThanZero(payment)) {
      return;
    }

    final BigDecimal coinsurancePercentage = coinsurance.divide(payment, 2, RoundingMode.HALF_UP);

    // If COIN percentage is less than or equal to the coinsurance cap then exit.
    if (BigDecimalUtils.isLessThanOrEqualTo(coinsurancePercentage, coinsuranceCap)) {
      return;
    }

    // Adjust coinsurance and reimbursement based on capped coinsurance amount.
    final BigDecimal offset =
        coinsurance.subtract(payment.multiply(coinsuranceCap)).setScale(2, RoundingMode.HALF_UP);
    line.setReimbursementAmount(reimbursement.add(offset));
    line.setCoinsuranceAmount(coinsurance.subtract(offset));
  }
}
