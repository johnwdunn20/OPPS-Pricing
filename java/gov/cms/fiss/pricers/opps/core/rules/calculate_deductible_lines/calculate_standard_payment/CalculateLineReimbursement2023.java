package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Stream;

public class CalculateLineReimbursement2023 extends AbstractDeductibleLineRule {

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

    // boolean to indicate REH coinsurance has been calculated. if boolean is true, normal
    // coinsurance logic sequence will be skipped below.
    boolean isREHCoinsuranceAlreadyCalculated = false;

    // local variable for sum of blood deductible and deductible
    BigDecimal sumOfDeductibleAndBloodDeductible =
        lineCalculation.getTotalDeductible().add(lineCalculation.getBloodDeductible());

    // Verify is REH line & line payment is NOT EQUAL TO blood deductible and deductible.
    // If both are true, proceed through REH coinsurance and reimbursement logic sequences.
    // WHEN DEDUCTIBLE & BLOOD DEDUCTIBLE DO NOT COMPLETELY COVER LINE PAYMENT
    if (calculationContext.getPricerContext().isRehExcludeCoinsurance()
        && !BigDecimalUtils.equals(
            lineCalculation.getPayment(), sumOfDeductibleAndBloodDeductible)) {

      // calculate the payment amount remaining after all deductibles are applied
      BigDecimal paymentDifference =
          lineCalculation
              .getPayment()
              .subtract(lineCalculation.getTotalDeductible())
              .subtract(lineCalculation.getBloodDeductible());

      // local variable for holding REH 5% Add-on amount for the line
      BigDecimal excludedFivePercentAddOnFromCoinsurance =
          calculationContext.getPricerContext().getRehFivePercentAddon();

      // If paymentDifference is GREATER THAN excludedFivePercentAddOnFromCoinsurance
      if (BigDecimalUtils.isGreaterThan(
          paymentDifference, excludedFivePercentAddOnFromCoinsurance)) {

        // When remaining Payment after Deductible & Blood Deductible are applied is GREATER THAN
        // the 5% add-on amount for the line. A portion of payment is subject to COINSURANCE.

        BigDecimal eligibleCoinsurance =
            paymentDifference.subtract(excludedFivePercentAddOnFromCoinsurance);
        // CALCULATE NATIONAL COINSURANCE
        lineCalculation.setNationalCoinsurance(
            eligibleCoinsurance
                .multiply((calculationContext.getPricerContext().getRehEligibleCoinsurance()))
                .setScale(2, RoundingMode.HALF_UP));

        // CALCULATE REIMBURSEMENT
        lineCalculation.setReimbursement(
            lineCalculation
                .getPayment() // payment includes 5% add-on already
                .subtract(lineCalculation.getTotalDeductible())
                .subtract(lineCalculation.getBloodDeductible())
                .subtract(lineCalculation.getNationalCoinsurance()) // added per Policy 2023 REH Fix
                .setScale(2, RoundingMode.HALF_UP));

        // set boolean to TRUE, REH coinsurance has been calculated
        isREHCoinsuranceAlreadyCalculated = true;

      }
      // When the 5% add on amount for line is GREATER THAN OR EQUAL TO the remaining Payment
      // after Deductible & Blood Deductible are applied.

      else {
        if (BigDecimalUtils.isGreaterThanOrEqualTo(
            excludedFivePercentAddOnFromCoinsurance, paymentDifference)) {

          // explicitly SET coinsurance to 0
          lineCalculation.setNationalCoinsurance(BigDecimalUtils.ZERO);

          // SET reimbursement using same calculation above
          lineCalculation.setReimbursement(
              lineCalculation
                  .getPayment() // payment includes 5% add-on already
                  .subtract(lineCalculation.getTotalDeductible())
                  .subtract(lineCalculation.getBloodDeductible())
                  .subtract(
                      lineCalculation.getNationalCoinsurance()) // added per Policy 2023 REH Fix
                  .setScale(2, RoundingMode.HALF_UP));

          // set boolean to TRUE, REH coinsurance has been calculated
          isREHCoinsuranceAlreadyCalculated = true;
        }
      }
      // 2023 logic update for REH-PassThroughDevice, si = 'H' & provider type = '24'
      if (isPassThroughDevice(lineInput.getStatusIndicator())) {
        // explicitly SET coinsurance to 0
        lineCalculation.setNationalCoinsurance(BigDecimalUtils.ZERO);
        // explicitly SET deductible to 0
        lineCalculation.setTotalDeductible(BigDecimalUtils.ZERO);
        // SET reimbursement using same calculation above
        lineCalculation.setReimbursement(
            lineCalculation
                .getPayment() // payment includes 5% add-on already
                .subtract(lineCalculation.getTotalDeductible())
                .subtract(lineCalculation.getBloodDeductible())
                .subtract(lineCalculation.getNationalCoinsurance()) // added per Policy 2023 REH Fix
                .setScale(2, RoundingMode.HALF_UP));

        // set boolean to TRUE, REH coinsurance has been calculated
        isREHCoinsuranceAlreadyCalculated = true;
      }

    } else {
      // FOR ALL NON-REH LINES PERFORM NORMAL REIMBURSEMENT CALCULATION
      // FOR REH LINES WHEN DEDUCTIBLE & BLOOD DEDUCTIBLE COMPLETELY COVER LINE PAYMENT
      // perform normal reimbursement calculation
      lineCalculation.setReimbursement(
          lineCalculation
              .getPayment()
              .subtract(lineCalculation.getTotalDeductible())
              .subtract(lineCalculation.getBloodDeductible())
              .multiply(deductibleLine.getReimbursementRate())
              .setScale(2, RoundingMode.HALF_UP));
    }
    // perform normal coinsurance calculation
    if (isREHCoinsuranceAlreadyCalculated == false) {
      // CALCULATE NATIONAL COINSURANCE
      // COMPUTE H-NAT-COIN = H-LITEM-PYMT - H-TOTAL-LN-DEDUCT - H-LITEM-REIM - H-LN-BLOOD-DEDUCT
      lineCalculation.setNationalCoinsurance(
          lineCalculation
              .getPayment()
              .subtract(lineCalculation.getTotalDeductible())
              .subtract(lineCalculation.getReimbursement())
              .subtract(lineCalculation.getBloodDeductible()));
    }
    // FOR SECTION 603 SERVICES -
    //  SET MIN, MAX, AND REDUCED COINSURANCE TO PSF STANDARD
    //  FOR LINES WITH A PMF= 7 OR 8 OR X OR Y
    // New for 2023, exclude  REH- Provider Type '24'
    // MAX coinsurance not calculated here intentionally. 603 Services always sets MAX = MIN
    // MAX coinsurance is not needed for 603 Services as this line will not go through
    // the AdjustMaximumNationalReducedCoinsurance rule
    if (OppsPricerContext.isSection603(lineInput.getPaymentMethodFlag())
        && !calculationContext.getPricerContext().isRehProviderType24Or25()) {

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

  private boolean isPassThroughDevice(String statusIndicator) {
    return Stream.of(StatusIndicator.H_PASS_THROUGH_DEVICE).anyMatch(si -> si.is(statusIndicator));
  }
}
