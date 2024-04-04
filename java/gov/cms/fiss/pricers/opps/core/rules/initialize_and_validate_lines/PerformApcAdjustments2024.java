package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.common.api.OutpatientProviderData;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import gov.cms.fiss.pricers.opps.core.codes.PaymentMethodFlag;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public class PerformApcAdjustments2024 extends AbstractLineCalculationRule {

  /** (Extracted from 19150-INIT). */
  @Override
  public void calculate(ServiceLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final IoceServiceLineData ioceServiceLine = calculationContext.getInput();
    final List<String> paymentAdjustmentFlags = ioceServiceLine.getPaymentAdjustmentFlags();
    final String paymentMethodFlag = ioceServiceLine.getPaymentMethodFlag();
    final OutpatientProviderData providerData =
        calculationContext.getPricerContext().getProviderData();

    //    1.	Check Provider Type = ‘24’
    //    2.	Check if Payment Method Flag = ‘8’ or 'A' or 'W' or 'Y'
    //    3.	Don’t Apply Reductions that correspond with Payment Method Flag = ‘8’, 'A', 'W' or 'Y'
    //    4.	Will not APPLY 20% Coinsurance Cap for 603 sv

    final String providerType = providerData.getProviderType();

    if (StringUtils.isNotBlank(providerType)
            && OppsPricerContext.REH_PROVIDER_TYPE_24.contains(providerType)
        // Add Provider Type = '25' for CY2024 : Non-OPPS Provider type
        || OppsPricerContext.REH_PROVIDER_TYPE_25.contains(providerType)) {
      pricerContext.setRehProviderType24Or25(true);
    }

    // New for CY 2023
    // 1.	Check if Rural Provider
    // 2.	Check Provider Type = ‘16’ or ‘17’ or ‘21’ or ‘22’
    // 3.	Check if Payment Method Flag = ‘A’ or 'W'
    // 4.	Don’t Pay 40% of APC Rate Off Campus Clinic PMF='A'
    // Will Pay 100% of APC Payment
    // Create boolean to be used to exclude RuralSCH from other APC adjustments

    if ((OppsPricerContext.isRuralCbsa(providerData.getCbsaActualGeographicLocation())
            || OppsPricerContext.isRuralCbsa(providerData.getCbsaWageIndexLocation())
            || OppsPricerContext.isRuralCbsa(providerData.getPaymentCbsa()))
        && StringUtils.isNotBlank(providerType)
        && OppsPricerContext.SCH_PROVIDER_TYPE.contains(providerType)) {

      pricerContext.setRuralSch(true);
    }
    // REDUCE APC PMT OF LINE WITH PMT METHOD FLAG = '8' or 'Y' (603 SV) SET THE COINSURANCE AND
    // REIMBURSEMENT TO PFS RATES
    // Exclude REH Provider type ='24' & '25'
    if (isFeeScheduleReducible(paymentMethodFlag) && !pricerContext.isRehProviderType24Or25()) {
      // COMPUTE H-APC-PYMT ROUNDED = H-APC-PYMT * PFS-REDUCT-2018
      pricerContext.setApcPayment(
          pricerContext
              .getApcPayment()
              .multiply(pricerContext.getPhysicianFeeScheduleReduction())
              .setScale(2, RoundingMode.HALF_UP));
    }
    // Add Radiation Oncology Model to have 20% insurance cap applied
    // Exclude REH Provider type ='24' & '25'
    if (OppsPricerContext.isSection603(paymentMethodFlag)
        || PaymentMethodFlag.RADIATION_ONCOLOGY_MODEL_B.is(paymentMethodFlag)
            && !pricerContext.isRehProviderType24Or25()) {

      // COMPUTE H-MIN-COIN ROUNDED = H-APC-PYMT * PFS-COIN-RATE
      // COMPUTE H-NAT-COIN ROUNDED = H-APC-PYMT * PFS-COIN-RATE
      final BigDecimal coinsurance =
          pricerContext
              .getApcPayment()
              .multiply(OppsPricerContext.PFS_REDUCTION_COINSURANCE_RATE)
              .setScale(2, RoundingMode.HALF_UP);
      pricerContext
          .getApcCalculationData()
          .setRate(OppsPricerContext.PFS_REDUCTION_REIMBURSEMENT_RATE);
      pricerContext.getApcCalculationData().setMinimumCoinsurance(coinsurance);
      pricerContext.getApcCalculationData().setNationalCoinsurance(coinsurance);
    }

    // REDUCE APC PMT OF LINE WITH PMT METHOD FLAG = 'A' PAY 40% OF APC RATE (section 603) or PMF
    // 'W"
    // Exclude RuralSCH Off Campus Clinic and Provider Type ='24' & '25' from reduction
    if (isOffCampusClinic(paymentMethodFlag)
        && !pricerContext.isRehProviderType24Or25()
        && !pricerContext.isRuralSch()) {
      // COMPUTE H-APC-PYMT ROUNDED = H-APC-PYMT * PMF-A-REDUCT-2020
      pricerContext.setApcPayment(
          pricerContext
              .getApcPayment()
              .multiply(pricerContext.getPaymentMethodFlagAReduction())
              .setScale(2, RoundingMode.HALF_UP));
    }
    // REDUCE APC PMT OF LINE WITH PMT ADJ FLAG = '14' (CT SCAN)
    if (PaymentAdjustmentFlag.CT_SCAN_14.is(paymentAdjustmentFlags)) {
      // COMPUTE H-APC-PYMT ROUNDED = H-APC-PYMT * CT-REDUCT-2017
      pricerContext.setApcPayment(
          pricerContext
              .getApcPayment()
              .multiply(pricerContext.getCTScanReduction())
              .setScale(2, RoundingMode.HALF_UP));
    }
    // REDUCE APC PMT OF LINE WITH PMT ADJ FLAG = '21' OR '23' (X-RAY SV)
    if (isXRayAdjustment(paymentAdjustmentFlags)) {
      // COMPUTE H-APC-PYMT ROUNDED = H-APC-PYMT * XRAY-FILM-REDUCT-2017
      pricerContext.setApcPayment(
          pricerContext
              .getApcPayment()
              .multiply(pricerContext.getXRayFilmReduction())
              .setScale(2, RoundingMode.HALF_UP));
    }
    // REDUCE APC PMT OF LINE WITH PMT ADJ FLAG = '22' OR '24' (X-RAY SV)
    if (isComputedRadiologyAdjustment(paymentAdjustmentFlags)) {
      // COMPUTE H-APC-PYMT ROUNDED = H-APC-PYMT * XRAY-CRT-REDUCT-2018
      pricerContext.setApcPayment(
          pricerContext
              .getApcPayment()
              .multiply(pricerContext.getXRayCRTReduction())
              .setScale(2, RoundingMode.HALF_UP));
    }

    // REDUCE APC PMT BY REDUCED UPDATE RATIO WHEN APPROPRIATE - 19180-REDUCE-APC-PYMT
    // Quality Reporting Reduction

    if (isEligibleForAPCReduction(
        ioceServiceLine, pricerContext.getProviderData().getHospitalQualityIndicator())) {
      // COMPUTE H-APC-PYMT ROUNDED = H-APC-PYMT * 0.981
      pricerContext.setApcPayment(
          pricerContext
              .getApcPayment()
              .multiply(pricerContext.getApcQualityReduction())
              .setScale(2, RoundingMode.HALF_UP));
      calculationContext.applyLineReturnCode(ReturnCode.ABSENT_QUALITY_REPORTING_11);
    }
  }

  /** APC Range Quality Indicator Reduction Exclusion. */
  private boolean isEligibleForAPCReduction(String statusIndicator, String apc) {
    final int numericAPC = Integer.parseInt(apc);

    if (!OppsPricerContext.isSeparatelyPayable(statusIndicator) || apc.length() != 5) {
      return false;
    }

    if (StatusIndicator.S_PROCEDURE_NOT_DISCOUNTED.is(statusIndicator)) {
      return !(numericAPC >= 1491 && numericAPC <= 1537)
          && !(numericAPC >= 1575 && numericAPC <= 1585)
          && !(numericAPC >= 1901 && numericAPC <= 1908)
          && !(numericAPC >= 6073 && numericAPC <= 6105);
    } else if (StatusIndicator.T_PROCEDURE_REDUCIBLE.is(statusIndicator)) {
      return !(numericAPC >= 1539 && numericAPC <= 1574)
          && !(numericAPC >= 1589 && numericAPC <= 1599)
          && !(numericAPC >= 1901 && numericAPC <= 1908);
    }

    return true;
  }

  /** Returns true if the service line is eligible for an APC reduction. */
  protected boolean isEligibleForAPCReduction(
      IoceServiceLineData ioceServiceLine, String hospitalQualityIndicator) {
    return !OppsPricerContext.isSection603(ioceServiceLine.getPaymentMethodFlag())
        // Exclude RO Model PMF 'B' from Quality Reduction
        && !PaymentMethodFlag.RADIATION_ONCOLOGY_MODEL_B.is(ioceServiceLine.getPaymentMethodFlag())
        && StringUtils.isEmpty(StringUtils.trimToEmpty(hospitalQualityIndicator))
        && isEligibleForAPCReduction(
            ioceServiceLine.getStatusIndicator(), ioceServiceLine.getPaymentApc());
  }

  private boolean isXRayAdjustment(List<String> paymentAdjustmentFlags) {
    return Stream.of(PaymentAdjustmentFlag.X_RAY_21, PaymentAdjustmentFlag.X_RAY_NO_COINSURANCE_23)
        .anyMatch(paf -> paf.is(paymentAdjustmentFlags));
  }

  private boolean isComputedRadiologyAdjustment(List<String> paymentAdjustmentFlag) {
    return Stream.of(
            PaymentAdjustmentFlag.COMPUTED_RADIOLOGY_22,
            PaymentAdjustmentFlag.COMPUTED_RADIOLOGY_NO_COINSURANCE_24)
        .anyMatch(paf -> paf.is(paymentAdjustmentFlag));
  }

  private boolean isFeeScheduleReducible(String paymentMethodFlag) {
    return Stream.of(
            PaymentMethodFlag.SECTION603_REDUCTION_8,
            PaymentMethodFlag.CONTRACTOR_BYPASS_SECTION_603_REDUCTION_Y)
        .anyMatch(pmf -> pmf.is(paymentMethodFlag));
  }

  private boolean isOffCampusClinic(String paymentMethodFlag) {
    return Stream.of(
            PaymentMethodFlag.OFF_CAMPUS_CLINIC_A,
            PaymentMethodFlag.CONTRACTOR_BYPASS_OFF_CAMPUS_CLINIC_W)
        .anyMatch(pmf -> pmf.is(paymentMethodFlag));
  }
}
