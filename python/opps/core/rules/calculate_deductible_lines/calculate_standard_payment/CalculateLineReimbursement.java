package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Stream;

public class CalculateLineReimbursement extends AbstractDeductibleLineRule {

  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final LineCalculation lineCalculation = calculationContext.getLineCalculation();
    final DeductibleLine deductibleLine = calculationContext.getInput();
    final IoceServiceLineData lineInput = deductibleLine.getServiceLine();
    /*
     * CALCULATE LINE REIMBURSEMENT
     */

    // FOR BLOOD DEDUCTIBLE LINES ON A COMPREHENSIVE APC CLAIM:
    // SET REIMBURSEMENT, COINSURANCE, & DEDUCTIBLE AMOUNTS TO $0
    if (calculationContext.isPackagedBloodDeductibleLine()) {
      lineCalculation.setReimbursement(BigDecimalUtils.ZERO);
      lineCalculation.setTotalDeductible(BigDecimalUtils.ZERO);
      lineCalculation.setNationalCoinsurance(BigDecimalUtils.ZERO);
      lineCalculation.setMinimumCoinsurance(BigDecimalUtils.ZERO);
      lineCalculation.setReducedCoinsurance(BigDecimalUtils.ZERO);

      calculationContext.completeStandardPaymentCalculation();
      return;
    }

    // CALCULATE REIMBURSEMENT AND SET COINSURANCE AMOUNTS TO $0 FOR LINES
    // WITH A PAF = 9 OR 10 OR 23 OR 24

    if (isCoinsuranceNotApplicable(lineInput.getPaymentAdjustmentFlags())) {

      // COMPUTE H-LITEM-REIM ROUNDED = H-LITEM-PYMT - H-TOTAL-LN-DEDUCT
      lineCalculation.setReimbursement(
          lineCalculation.getPayment().subtract(lineCalculation.getTotalDeductible()));
      lineCalculation.setNationalCoinsurance(BigDecimalUtils.ZERO);
      lineCalculation.setMinimumCoinsurance(BigDecimalUtils.ZERO);
      lineCalculation.setReducedCoinsurance(BigDecimalUtils.ZERO);

      calculationContext.completeStandardPaymentCalculation();
      return;
    }

    // STANDARD LINE REIMBURSEMENT CALCULATION
    // COMPUTE H-LITEM-REIM ROUNDED =
    //                ((H-LITEM-PYMT - H-TOTAL-LN-DEDUCT) -
    //                  H-LN-BLOOD-DEDUCT) * W-PPCT (W-LP-INDX)
    lineCalculation.setReimbursement(
        lineCalculation
            .getPayment()
            .subtract(lineCalculation.getTotalDeductible())
            .subtract(lineCalculation.getBloodDeductible())
            .multiply(deductibleLine.getReimbursementRate())
            .setScale(2, RoundingMode.HALF_UP));

    // CALCULATE NATIONAL COINSURANCE
    // COMPUTE H-NAT-COIN = H-LITEM-PYMT - H-TOTAL-LN-DEDUCT - H-LITEM-REIM - H-LN-BLOOD-DEDUCT
    lineCalculation.setNationalCoinsurance(
        lineCalculation
            .getPayment()
            .subtract(lineCalculation.getTotalDeductible())
            .subtract(lineCalculation.getReimbursement())
            .subtract(lineCalculation.getBloodDeductible()));

    // FOR SECTION 603 SERVICES -
    //  SET MIN, MAX, AND REDUCED COINSURANCE TO PSF STANDARD
    //  FOR LINES WITH A PMF= 7 OR 8 OR X OR Y
    // MAX coinsurance not calculated here intentionally. 603 Services always sets MAX = MIN
    // MAX coinsurance is not needed for 603 Services as this line will not go through
    // the AdjustMaximumNationalReducedCoinsurance rule
    if (OppsPricerContext.isSection603(lineInput.getPaymentMethodFlag())) {

      // COMPUTE H-MIN-COIN ROUNDED = H-LITEM-PYMT * PFS-COIN-RATE
      lineCalculation.setMinimumCoinsurance(
          lineCalculation
              .getPayment()
              .multiply(OppsPricerContext.PFS_REDUCTION_COINSURANCE_RATE)
              .setScale(2, RoundingMode.HALF_UP));
      lineCalculation.setReducedCoinsurance(BigDecimalUtils.ZERO);

      calculationContext.completeStandardPaymentCalculation();
    }
  }

  private boolean isCoinsuranceNotApplicable(List<String> paymentAdjustmentFlags) {
    return Stream.of(
            PaymentAdjustmentFlag.DEDUCTIBLE_AND_COINSURANCE_NOT_APPLICABLE_9,
            PaymentAdjustmentFlag.COINSURANCE_NOT_APPLICABLE_10,
            PaymentAdjustmentFlag.X_RAY_NO_COINSURANCE_23,
            PaymentAdjustmentFlag.COMPUTED_RADIOLOGY_NO_COINSURANCE_24)
        .anyMatch(si -> si.is(paymentAdjustmentFlags));
  }
}
