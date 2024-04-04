package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.calculate_status_indicator_payments;

import gov.cms.fiss.pricers.common.api.OutpatientProviderData;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import gov.cms.fiss.pricers.opps.core.codes.PaymentMethodFlag;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.AbstractDeductibleLineRule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractStatusIndicatorPayments2023 extends AbstractDeductibleLineRule {
  private static final Pattern BILL_TYPE = Pattern.compile("14[0-9A-Z]");

  /**
   * CALCULATE THE BENEFICIARY DEDUCTIBLE AMOUNT THAT WILL BE APPLIED TO THE SERVICE LINES IN THE
   * DEDUCTIBLE TABLE THE DEDUCTIBLE IS APPLIED TO THE LOWEST TO HIGHEST RANKED APCS. THE LOWER THE
   * RANK THE HIGHER THE COINSURANCE %. THE TOTAL CLAIM BENEFICIARY COINSURANCE AMOUNT IS CHEAPER
   * WHEN THE DEDUCTIBLE IS APPLIED IN THIS ORDER.
   *
   * <p>(19560-CALC-BENE-DEDUCT)
   */
  protected BigDecimal calculateBeneficiaryDeductible(
      LineCalculation lineCalculation, BigDecimal beneficiaryDeductible) {

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    final ServiceLinePaymentData lineOutput = lineCalculation.getLineOutput();

    // LINES INELIGIBLE FOR DEDUCTIBLE SKIP DEDUCTIBLE CALCULATION
    if (!isDeductibleAdjustment(lineInput.getPaymentAdjustmentFlags())) {
      return beneficiaryDeductible;
    }

    BigDecimal bloodPayment = BigDecimalUtils.ZERO; // H-LN-BLD-PYMT

    // IF BENEFICIARY HAS NOT MET HIS/HER DEDUCTIBLE LIMIT THEN CALCULATE THE LINE BLOOD PAYMENT
    if (BigDecimalUtils.isGreaterThanZero(beneficiaryDeductible)) {
      // COMPUTE H-LN-BLD-PYMT = H-LITEM-PYMT - H-LN-BLOOD-DEDUCT
      bloodPayment = lineCalculation.getPayment().subtract(lineCalculation.getBloodDeductible());
    }

    // BENEFICIARY'S DEDUCTIBLE DOES NOT COVER OR JUST COVERS THE
    // ENTIRE LINE BLOOD PAYMENT:
    // - BENEFICIARY'S REMAINING DEDUCTIBLE AMT APPLIED TO LINE
    // - BENEFICIARY HAS REACHED HIS/HER DEDUCTIBLE LIMIT
    if (BigDecimalUtils.isGreaterThanOrEqualTo(bloodPayment, beneficiaryDeductible)) {
      lineCalculation.setTotalDeductible(BigDecimalUtils.defaultValue(beneficiaryDeductible));
      return BigDecimalUtils.ZERO;
    }
    // BENEFICIARY'S DEDUCTIBLE MORE THAN COVERS THE LINE BLOOD
    // PAYMENT, DO THE FOLLOWING:
    // - CALCULATE THE BENEFICIARY'S REMAINING DEDUCTIBLE AMOUNT
    //   AFTER PAYING FOR CURRENT SERVICE LINE
    // - MEDICARE LINE PAYMENT = 0
    else {
      lineCalculation.setTotalDeductible(bloodPayment);
      lineOutput.setReturnCode(ReturnCode.PAYMENT_EQUALS_ZERO_20.toReturnCodeData());
      // COMPUTE H-BENE-DEDUCT = H-BENE-DEDUCT - H-LN-BLD-PYMT
      return beneficiaryDeductible.subtract(bloodPayment);
    }
  }

  /**
   * CALCULATE WAGE ADJUSTED LINE ITEM PAYMENT WITH RURAL SOLE COMMUNITY HOSPITAL (SCH) ADJUSTMENT
   * WHEN APPLICABLE (FOR LINES WITH A SI OF S, V, T, P, X, R, OR U )
   *
   * <pre>
   * THE SCH ADJUSTMENT IS MADE WHEN THE FOLLOWING IS TRUE:
   *       EITHER THE L-PSF-GEO-CBSA OR THE L-PSF-WI-CBSA OR THE
   *       L-PSF-PYMT-CBSA MUST BE A VALUE OF
   *       '   01' THRU '   99' AND THE L-PSF-PROV-TYPE
   *       MUST BE A '16' OR '17' OR '21' OR '22'.
   * </pre>
   *
   * <p>(19550-SCH-ADJ)
   */
  protected void calculateWageAdjustedPaymentAndSchAdj(
      OppsPricerContext calculationContext,
      OutpatientProviderData providerData,
      LineCalculation lineCalculation,
      String billType) {

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    final DeductibleLine deductibleLine = lineCalculation.getDeductibleLine();

    final String si = lineInput.getStatusIndicator();
    final List<String> paymentAdjustmentFlags = lineInput.getPaymentAdjustmentFlags();
    final BigDecimal soleCommunityHospital; // H-SCH-PYMT

    // CALCULATE THE SCH PAYMENT
    //      - FOR SCHS = APC OR BLOOD APC PAYMENT ADJUSTED BY 7.1%
    //      - FOR NON-SCHS = UNADJUSTED APC OR BLOOD APC PAYMENT
    //      - SECTION 603 SERVICES EXCLUDED FROM THE SCH ADJUSTMENT

    // SCH
    if ((OppsPricerContext.isRuralCbsa(providerData.getCbsaActualGeographicLocation())
            || OppsPricerContext.isRuralCbsa(providerData.getCbsaWageIndexLocation())
            || OppsPricerContext.isRuralCbsa(providerData.getPaymentCbsa()))
        && StringUtils.isNotBlank(providerData.getProviderType())
        && OppsPricerContext.SCH_PROVIDER_TYPE.contains(providerData.getProviderType())
        && !isBillType14x(billType)
        && !OppsPricerContext.isSection603(lineInput.getPaymentMethodFlag())) {

      // BLOOD DEDUCTIBLE HCPCS LINE

      // COMPUTE H-SCH-PYMT ROUNDED = (W-BD-APC-PYMT (W-BD-INDX) * 1.071)
      soleCommunityHospital =
          deductibleLine
              .getApcPayment()
              .multiply(calculationContext.getSoleCommunityHospitalAdjustmentRate())
              .setScale(2, RoundingMode.HALF_UP);
    } else { // Non-SCH
      soleCommunityHospital = deductibleLine.getApcPayment();
    }

    // CALCULATE THE LINE ITEM PAYMENT

    // SI = R (BLOOD) OR U (BRACHY) LINES ARE NOT WAGE-ADJUSTED
    if (isBloodProductOrBrachytherapy(si)) {
      // Compute statements have been reduced since COIN & Blood deductible entries have been
      // consolidated
      // COMPUTE H-LITEM-PYMT ROUNDED =
      //     H-SCH-PYMT *
      //     W-SRVC-UNITS (W-LP-INDX) *
      //     W-DISC-RATE (W-LP-INDX)
      lineCalculation.setPayment(
          soleCommunityHospital
              .multiply(new BigDecimal(deductibleLine.getServiceUnits()))
              .multiply(deductibleLine.getDiscountRate())
              .setScale(2, RoundingMode.HALF_UP));

      // REH 5% Add-on logic
      //    1.	Check Provider Type = ‘24’
      //    2.	Exclude Status Indicators = ‘A‘, ‘F’, ‘G‘, ‘K‘, ‘L’
      rehFivePercentAddOn(calculationContext, providerData, lineCalculation, si);
    }
    // SI = S, V, T, P, X, J1, OR J2 LINES ARE WAGE-ADJUSTED (60%)
    else {
      // COMPUTE H-LITEM-PYMT ROUNDED =
      //                          (((H-SCH-PYMT * .60) *
      //                               W-WINX (W-LP-INDX)) +
      //                            (H-SCH-PYMT * .40)) *
      //                          W-SRVC-UNITS (W-LP-INDX) *
      //                          W-DISC-RATE (W-LP-INDX)
      lineCalculation.setPayment(
          soleCommunityHospital
              .multiply(new BigDecimal(".60"))
              .multiply(deductibleLine.getWageIndex())
              .add(soleCommunityHospital.multiply(new BigDecimal(".40")))
              .multiply(new BigDecimal(deductibleLine.getServiceUnits()))
              .multiply(deductibleLine.getDiscountRate())
              .setScale(2, RoundingMode.HALF_UP));

      // REH 5% Add-on logic
      //    1.	Check Provider Type = ‘24’
      //    2.	Exclude Status Indicators = ‘A‘, ‘F’, ‘G‘, ‘K‘, ‘L’
      rehFivePercentAddOn(calculationContext, providerData, lineCalculation, si);

      //    If RO Model- PMA adjustment to be multiplied by product of Sole Community Hospital
      //    adjustment above
      //    VALIDATION FOR pma NEEDED, if PMA is numeric and > 0
      //    If OPPS-SITE-SRVC-FLAG (LN-SUB) = 'B'
      //    Litem-pymt = litem-pymt * L-PSF-PYMT-MODEL-ADJ (rounded)

      if (PaymentMethodFlag.RADIATION_ONCOLOGY_MODEL_B.is(lineInput.getPaymentMethodFlag())
          && BigDecimalUtils.isGreaterThanZero(providerData.getPaymentModelAdjustment())) {
        lineCalculation.setPayment(
            lineCalculation
                .getPayment()
                .multiply(providerData.getPaymentModelAdjustment())
                .setScale(2, RoundingMode.HALF_UP));
      }
    }

    // REDUCE ADJUSTED APC PAYMENT BY DEVICE CREDIT IF APPLICABLE
    if (PaymentAdjustmentFlag.DEVICE_CREDIT_17.is(paymentAdjustmentFlags)
        && BigDecimalUtils.isGreaterThanZero(lineCalculation.getDeviceCreditAmount())) {
      if (BigDecimalUtils.isGreaterThanOrEqualTo(
          lineCalculation.getPayment(), lineCalculation.getDeviceCreditAmount())) {

        //  COMPUTE H-LITEM-PYMT ROUNDED = H-LITEM-PYMT - H-LINE-DEVCR-AMT
        lineCalculation.setPayment(
            lineCalculation.getPayment().subtract(lineCalculation.getDeviceCreditAmount()));
      } else {
        lineCalculation.setPayment(BigDecimalUtils.ZERO);
      }
    }
  }

  // Method created for 5 percent Add on payment for REH - 2023
  protected static void rehFivePercentAddOn(
      OppsPricerContext calculationContext,
      OutpatientProviderData providerData,
      LineCalculation lineCalculation,
      String si) {
    //    1.	Check Provider Type = ‘24’
    //    2.	Exclude Status Indicators = ‘A‘, ‘F’, ‘G‘, ‘K‘, ‘L’
    if (StringUtils.isNotBlank(providerData.getProviderType())
        && OppsPricerContext.REH_PROVIDER_TYPE_24.contains(providerData.getProviderType())
        && !calculationContext.isREHStatusIndicatorExclusion(si)) {
      //      4.	No coinsurance will be applied to 5% add-on payment
      //      5.	Apply coinsurance to payment excluding 5% add-on
      // Hold variable for 5% add-on payment amount
      final BigDecimal rehFivePercentAddon =
          lineCalculation
              .getPayment()
              .multiply(calculationContext.getREHApplyFivePercentAddOn())
              .setScale(2, RoundingMode.HALF_UP);

      calculationContext.setRehFivePercentAddon(rehFivePercentAddon);
      // boolean will indicate within coinsurance logic to exclude 5% add-on
      calculationContext.setRehExcludeCoinsurance(true);
      //      3.	Apply 5% add-on payment in  Logic
      // Add 5% add on to Payment
      lineCalculation.setPayment(
          lineCalculation.getPayment().add(rehFivePercentAddon).setScale(2, RoundingMode.HALF_UP));
    }
  }

  private boolean isDeductibleAdjustment(List<String> paymentAdjustmentFlags) {
    return Stream.of(
            PaymentAdjustmentFlag.DEDUCTIBLE_NOT_APPLICABLE_4,
            PaymentAdjustmentFlag.DEDUCTIBLE_AND_COINSURANCE_NOT_APPLICABLE_9,
            PaymentAdjustmentFlag.X_RAY_NO_COINSURANCE_23,
            PaymentAdjustmentFlag.COMPUTED_RADIOLOGY_NO_COINSURANCE_24,
            PaymentAdjustmentFlag.COLONIAL_PROCEDURE_25)
        .noneMatch(si -> si.is(paymentAdjustmentFlags));
  }

  /** Determines if the current line is a blood, blood product or brachytherapy. */
  public boolean isBloodProductOrBrachytherapy(String statusIndicator) {
    return Stream.of(StatusIndicator.R_BLOOD, StatusIndicator.U_BRACHYTHERAPY)
        .anyMatch(si -> si.is(statusIndicator));
  }

  /** Return true if bill type is prefixed with 14. */
  private boolean isBillType14x(String billType) {
    return BILL_TYPE.matcher(billType).matches();
  }
}
