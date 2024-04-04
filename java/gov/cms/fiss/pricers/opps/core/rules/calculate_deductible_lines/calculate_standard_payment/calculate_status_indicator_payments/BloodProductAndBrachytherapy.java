package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.calculate_status_indicator_payments;

import gov.cms.fiss.pricers.common.api.OutpatientProviderData;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import gov.cms.fiss.pricers.opps.core.codes.PaymentIndicator;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Stream;

public class BloodProductAndBrachytherapy extends AbstractStatusIndicatorPayments2024 {

  /** CALCULATE PAYMENT FOR SI = R &amp; U LINES. */
  @Override
  public boolean shouldExecute(DeductibleLineContext context) {
    return isBloodProductOrBrachytherapy(context.getStatusIndicator());
  }

  /**
   * Calculates payment based on status indicators.
   *
   * <p>(Extracted from 19550-CALC-STANDARD)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final LineCalculation lineCalculation = calculationContext.getLineCalculation();
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final OutpatientProviderData providerData = pricerContext.getProviderData();
    final IoceServiceLineData ioceServiceLine = lineCalculation.getLineInput();
    final String paymentIndicator = ioceServiceLine.getPaymentIndicator();
    final String billType = pricerContext.getClaimData().getTypeOfBill();

    // CALCULATE PAYMENT FOR SI = R & U LINES; THE PMT. IND.
    // SHOULD BE 1, 5, OR 7 FOR THESE LINES (PAY STANDARD AMOUNT)
    // RETURN ERROR CODE 41 IF INVALID SI - PYMT IND. COMBO
    if (isPayStandardAmountIndicator(paymentIndicator)) {
      pricerContext.setBeneficiaryBloodPintsUsed(
          calculateBloodAndBrachyLines(
              pricerContext,
              providerData,
              lineCalculation,
              billType,
              calculationContext.isPackagedBloodDeductibleLine(),
              pricerContext.getBeneficiaryBloodPintsUsed(),
              pricerContext.getBloodProductPercentage()));

      if (!calculationContext.isPackagedBloodDeductibleLine()) {
        pricerContext.setBeneficiaryDeductible(
            calculateBeneficiaryDeductible(
                lineCalculation, pricerContext.getBeneficiaryDeductible()));
      }
    } else {
      calculationContext.applyLineReturnCode(ReturnCode.STATUS_INDICATOR_INVALID_FOR_OPPS_41);
    }
  }

  /**
   * CALCULATE THE FOLLOWING FOR VALID SI = R &amp; U LINES:
   *
   * <pre>
   *         - APC PAYMENT FOR BLOOD LINES (SI = R)
   *         - BLOOD SPECIFIC ITEMS FOR BLOOD LINES
   *         - LINE ITEM PMT FOR ALL SI = U LINES
   *         - SCH ADJUSTMENT APPLIED TO BLOOD &amp; BRACHY LINES
   *         - BLOOD DEDUCTIBLE FOR BLOOD DEDUCTIBLE HCPCS LINES
   * </pre>
   *
   * <p>(19550-CALC-RU).
   */
  protected int calculateBloodAndBrachyLines(
      OppsPricerContext calculationContext,
      OutpatientProviderData providerData,
      LineCalculation lineCalculation,
      String billType,
      boolean packagedBloodDeductibleLineFlag,
      int beneficiaryBloodPintsUsed,
      BigDecimal deductibleBloodPercentage) {

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();

    final String statusIndicator = lineInput.getStatusIndicator();
    final String hcpcs = lineInput.getHcpcsCode();
    final List<String> paymentAdjustmentFlags = lineInput.getPaymentAdjustmentFlags();

    // CALCULATE LINE ITEM PAYMENT & BLOOD DEDUCTIBLE FOR BLOOD
    // LINE WITH A HCPCS SUBJECT TO THE BLOOD DEDUCTIBLE (BD)
    //
    // (BLOOD DEDUCTIBLE LINES ARE PROCESSED IN THE ORDER THEY
    //  APPEAR IN THE BLOOD DEDUCTIBLE TABLE - EARLIEST TO LATEST
    //  DATE, LEAST TO MOST EXPENSIVE PAYMENT RATE.  THE CURRENT
    //  COINSURANCE DEDUCTIBLE TABLE LINE BEING PROCESSED DOES NOT
    //  NECESSARILY CORRESPOND TO THE BLOOD DEDUCTIBLE LINE
    //  PROCESSED IN THE LOGIC BELOW.)
    if (calculationContext.isBloodDeductibleHcpcsLine(
        statusIndicator, hcpcs, paymentAdjustmentFlags)) {

      // CALCULATE BLOOD FRACTION & BLOOD PINTS USED
      beneficiaryBloodPintsUsed =
          calculateBloodFraction(lineCalculation, beneficiaryBloodPintsUsed);

      // ADJUST BLOOD APC PAYMENT BY BLOOD PRODUCT OR LABOR RATE
      adjustBloodCost(lineCalculation, deductibleBloodPercentage);

      // CALCULATE LINE ITEM PAYMENT W/ SCH ADJUSTMENT IF APPLICABLE
      calculateWageAdjustedPaymentAndSchAdj(
          calculationContext, providerData, lineCalculation, billType);

      // CALCULATE BLOOD DEDUCTIBLE (BD) - H-BLOOD-FRACTION IS THE
      // FRACTION OF THE CURRENT LINE'S PINTS COVERED BY THE BD

      // COMPUTE H-LN-BLOOD-DEDUCT ROUNDED = H-LITEM-PYMT * H-BLOOD-FRACTION
      lineCalculation.setBloodDeductible(
          lineCalculation
              .getPayment()
              .multiply(lineCalculation.getBloodFraction())
              .setScale(2, RoundingMode.HALF_UP));

      // CHANGE THE PAYMENT OF THE BLOOD DEDUCTIBLE LINE ON THE SAME
      // CLAIM AS A COMPREHENSIVE APC TO THE BLOOD DEDUCTIBLE AMOUNT
      if (packagedBloodDeductibleLineFlag) {
        lineCalculation.setPayment(lineCalculation.getBloodDeductible());
      }
    }
    // CALCULATE LINE ITEM PAYMENT FOR BLOOD LINE WITH A HCPCS NOT
    // SUBJECT TO THE BLOOD DEDUCTIBLE, BUT PAF = 5 OR 6
    // (BLOOD PROCESSING AND PRODUCT BILLED TOGETHER)
    else {
      // ADJUST APC PAYMENT BY BLOOD PRODUCT OR LABOR RATE
      if (StatusIndicator.R_BLOOD.is(statusIndicator)
          && OppsPricerContext.isBloodPaymentAdjustment(paymentAdjustmentFlags)) {
        adjustBloodCost(lineCalculation, deductibleBloodPercentage);
      }
      // CALCULATE LINE ITEM PAYMENT W/ SCH ADJUSTMENT IF APPLICABLE
      calculateWageAdjustedPaymentAndSchAdj(
          calculationContext, providerData, lineCalculation, billType);
    }

    return beneficiaryBloodPintsUsed;
  }

  /**
   * DETERMINE THE FRACTION OF THE BLOOD LINE'S PAYMENT THAT WILL BE COVERED BY THE THE
   * BENEFICIARY'S BLOOD DEDUCTIBLE FOR LINES WITH A HCPCS IN THE BLOOD DEDUCTIBLE LIST
   *
   * <p>THE BENEFICIARY PAYS A BLOOD DEDUCTIBLE FOR THE FIRST 3 CHEAPEST BLOOD PINTS. MEDICARE
   * COVERS ANY ADDITIONAL PINTS USED BY THE BENEFICIARY.
   *
   * <p>(19550-SET-BLOOD-FRACTION)
   */
  protected int calculateBloodFraction(
      LineCalculation lineCalculation, int beneficiaryBloodPintsUsed) {

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    final DeductibleLine deductibleLine = lineCalculation.getDeductibleLine();

    lineCalculation.setBloodFraction(BigDecimalUtils.ZERO);

    // EXIT IF NOT A BLOOD/BLOOD PRODUCT LINE OR NO BLOOD DEDUCTIBLE PINTS LEFT
    if (!PaymentAdjustmentFlag.BLOOD_DEDUCTIBLE_5.is(lineInput.getPaymentAdjustmentFlags())
        || beneficiaryBloodPintsUsed == 0) {
      return beneficiaryBloodPintsUsed;
    }

    // BENEFICIARY HAS ENOUGH PINTS TO COVER ALL BLOOD LINE UNITS
    //  - BLOOD DEDUCTIBLE WILL COVER THE ENTIRE PYMNT (FRAC. = 1)
    //  - CALCULATE THE NUMBER OF DEDUCTIBLE PINTS BENE HAS LEFT
    if (deductibleLine.getServiceUnits() <= beneficiaryBloodPintsUsed) {
      lineCalculation.setBloodFraction(new BigDecimal("1.00"));

      // COMPUTE H-BENE-PINTS-USED = H-BENE-PINTS-USED - W-BD-SRVC-UNITS (W-BD-INDX)
      beneficiaryBloodPintsUsed -= deductibleLine.getServiceUnits();
    }
    // BENE. DOESN'T HAVE ENOUGH PINTS TO COVER ENTIRE BLOOD LINE
    //   - BLOOD DEDUCTIBLE WILL COVER A FRACTION OF THE PAYMENT
    //     (ACCORDING TO THE % OF PINTS COVERED)
    //   - BENEFICIARY HAS NO DEDUCTIBLE PINTS LEFT (PINTS = 0)
    else {

      // COMPUTE H-BLOOD-FRACTION = H-BENE-PINTS-USED / W-BD-SRVC-UNITS (W-BD-INDX)
      lineCalculation.setBloodFraction(
          new BigDecimal(beneficiaryBloodPintsUsed)
              .divide(new BigDecimal(deductibleLine.getServiceUnits()), 8, RoundingMode.DOWN));
      beneficiaryBloodPintsUsed = 0;
    }

    return beneficiaryBloodPintsUsed;
  }

  /**
   * ADJUST APC PAYMENT BY BLOOD PRODUCT OR LABOR RATE TO CALCULATE THE BLOOD APC PAYMENT FOR LINES
   * WITH A HCPCS IN THE BLOOD DEDUCTIBLE LIST
   *
   * <p>THE RATE OF BLOOD TO BLOOD LABOR IS THE CLAIM AVERAGE. THE BLOOD LINE IS PAID ONLY THE
   * PORTION OF THE APC PAYMENT THAT IS INDICATED BY THE PYMNT ADJUSTMENT FLAG.
   *
   * <p>(19550-ADJ-BLOOD-COST) (19550-ADJ-PLATE-COST)
   */
  protected void adjustBloodCost(
      LineCalculation lineCalculation, BigDecimal deductibleBloodPercentage) {

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    final DeductibleLine deductibleLine = lineCalculation.getDeductibleLine();

    final List<String> paymentAdjustmentFlags = lineInput.getPaymentAdjustmentFlags();
    // CALCULATE BLOOD APC PAYMENT FOR BLOOD/BLOOD PRODUCT LINE
    if (PaymentAdjustmentFlag.BLOOD_DEDUCTIBLE_5.is(paymentAdjustmentFlags)) {

      // COMPUTE W-BD-APC-PYMT (W-BD-INDX) = W-BD-APC-PYMT (W-BD-INDX) * H-38X-39X-RATE
      deductibleLine.setApcPayment(
          deductibleLine
              .getApcPayment()
              .multiply(deductibleBloodPercentage)
              .setScale(2, RoundingMode.DOWN));
    }

    // CALCULATE BLOOD APC PAYMENT FOR BLOOD PROCESS/STORAGE LINE
    if (PaymentAdjustmentFlag.BLOOD_NOT_DEDUCTIBLE_6.is(paymentAdjustmentFlags)) {

      // COMPUTE W-BD-APC-PYMT (W-BD-INDX) = W-BD-APC-PYMT (W-BD-INDX) * (1 - H-38X-39X-RATE)
      deductibleLine.setApcPayment(
          deductibleLine
              .getApcPayment()
              .multiply(BigDecimal.ONE.subtract(deductibleBloodPercentage))
              .setScale(2, RoundingMode.DOWN));
    }
  }

  /** Determines if the given payment indicator is categorized as a pay standard amount. */
  private boolean isPayStandardAmountIndicator(String paymentIndicator) {
    return Stream.of(
            PaymentIndicator.PAID_STANDARD_HOSPITAL_OPPS_AMOUNT_1,
            PaymentIndicator.PAID_STANDARD_AMOUNT_FOR_PASS_THROUGH_DRUG_OR_BIOLOGICAL_5,
            PaymentIndicator.ADDITIONAL_PAYMENT_FOR_NEW_DRUG_OR_BIOLOGICAL_7)
        .anyMatch(pi -> pi.is(paymentIndicator));
  }
}
