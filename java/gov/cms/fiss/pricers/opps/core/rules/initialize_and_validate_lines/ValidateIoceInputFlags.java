package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import gov.cms.fiss.pricers.opps.core.codes.ActionFlag;
import gov.cms.fiss.pricers.opps.core.codes.PackageFlag;
import gov.cms.fiss.pricers.opps.core.codes.PaymentAdjustmentFlag;
import gov.cms.fiss.pricers.opps.core.codes.PaymentIndicator;
import gov.cms.fiss.pricers.opps.core.codes.PaymentMethodFlag;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import java.util.List;
import java.util.stream.Stream;

public class ValidateIoceInputFlags extends AbstractLineCalculationRule {

  /**
   * CHECK LINE OCE VALUES FOR VALIDITY.
   *
   * <p>(Extracted from 19150-INIT)
   */
  @Override
  public void calculate(ServiceLineContext calculationContext) {
    final IoceServiceLineData ioceServiceLine = calculationContext.getInput();
    final String statusIndicator = ioceServiceLine.getStatusIndicator();
    final String lineActionFlag = ioceServiceLine.getActionFlag();

    // IDENTIFY ALL VALID SERVICE INDICATORS (SI) & RETURN ERROR CODE 40 IF THE SI IS INVALID.
    if (!isValidStatusIndicator(statusIndicator)) {
      calculationContext.applyLineReturnCode(ReturnCode.STATUS_INDICATOR_INVALID_40);
      return;
    }

    // IDENTIFY SERVICE INDICATORS (SI) NOT VALID FOR THE OPPS PRICER & RETURN ERROR CODE 41 IF THE
    // SI IS INVALID FOR THE OPPS PRICER.
    if (!isValidStatusIndicatorForOPPS(statusIndicator)) {
      calculationContext.applyLineReturnCode(ReturnCode.STATUS_INDICATOR_INVALID_FOR_OPPS_41);
      return;
    }

    // IDENTIFY VALID PAYMENT INDICATORS & RETURN ERROR CODE 43 IF THE PAYMENT INDICATOR IS INVALID
    if (!isValidPaymentIndicator(ioceServiceLine.getPaymentIndicator())) {
      calculationContext.applyLineReturnCode(ReturnCode.INVALID_PAYMENT_INDICATOR_43);
      return;
    }

    // IDENTIFY VALID PACKAGING FLAGS & RETURN ERROR CODE 45 IF THE PACKAGING FLAG IS INVALID.
    if (!isValidPackageFlag(ioceServiceLine.getPackageFlag())) {
      calculationContext.applyLineReturnCode(ReturnCode.PACKAGING_FLAG_NOT_ZERO_45);
      return;
    }

    // RETURN ERROR CODE 46 IF THE D/R FLAG OR D/R FLAG-HCPCS COMBO IS INVALID.
    if (!isValidDenialOrRejection(ioceServiceLine.getDenyOrRejectFlag(), lineActionFlag)) {
      calculationContext.applyLineReturnCode(ReturnCode.INVALID_DENIAL_46);
      return;
    }

    // RETURN ERROR CODE 47 IF THE LINE ITEM ACTION FLAG IS INVALID.
    if (!isValidActionFlag(lineActionFlag)) {
      calculationContext.applyLineReturnCode(ReturnCode.INVALID_ACTION_FLAG_47);
      return;
    }

    // IDENTIFY VALID PAYMENT ADJUSTMENT FLAGS (PAF) & RETURN ERROR CODE 48 IF THE PAF IS INVALID.
    if (!isValidAdjustmentFlag(ioceServiceLine.getPaymentAdjustmentFlags())) {
      calculationContext.applyLineReturnCode(ReturnCode.INVALID_PAYMENT_ADJUSTMENT_FLAG_48);
      return;
    }

    //  IDENTIFY VALID SITE OF SERVICE (SOS) FLAG AND CASES
    //  WHERE THE SOS FLAG IS IGNORED & RETURN ERROR CODE 49 IF
    //  THE SOS FLAG IS INVALID AND NOT IGNORED.
    //
    //  ** SITE OF SERVICE FLAG = PAYMENT METHOD FLAG IN OCE **
    //
    //  NOTE: PHP = PARTIAL HOSPITALIZATION
    //        WHEN SI = 'P', PHP APC IS ON THE CURRENT LINE
    if (!isValidPaymentMethodFlag(ioceServiceLine.getPaymentMethodFlag())) {
      calculationContext.applyLineReturnCode(ReturnCode.BILL_INCLUSION_FLAG_NOT_ZERO_49);
    }

    // IF MISSING OR INVALID DISCOUNT FACTOR SET RETURN CODE TO '38'
    if (!isValidDiscountFormula(ioceServiceLine.getDiscountingFormula())) {
      calculationContext.applyLineReturnCode(ReturnCode.DISCOUNT_FACTOR_INDICATOR_INVALID_38);
      return;
    }

    // Default null date of service lines to claim service date (required since IOCE fields are
    // optional PR #144)
    if (ioceServiceLine.getDateOfService() == null) {
      ioceServiceLine.setDateOfService(
          calculationContext.getPricerContext().getClaimData().getServiceFromDate());
    }
  }

  /**
   * IDENTIFY VALID LINE ITEM DENIAL OR REJECTION (D/R) FLAGS AND VALID D/R FLAG-HCPCS COMBINATIONS.
   *
   * @param drFlag String Denial or Rejection Flag
   * @param lineActionFlag String line Action flag
   * @return boolean true if valid denial or rejection flag and/or combo
   */
  private boolean isValidDenialOrRejection(String drFlag, String lineActionFlag) {
    return drFlag != null && OppsPricerContext.LINE_DENY_OR_REJECT_FLAG.contains(drFlag)
        || ActionFlag.LINE_ITEM_DENIAL_OR_REJECTION_IS_IGNORED_1.is(lineActionFlag);
  }

  /**
   * IDENTIFY INVALID LINE ITEM ACTION FLAGS.
   *
   * @param lineActionFlag line Action flag
   * @return boolean true if lineActionFlag is not 2 or 3
   */
  private boolean isValidActionFlag(String lineActionFlag) {
    return Stream.of(
            ActionFlag.EXTERNAL_LINE_ITEM_DENIAL_2, ActionFlag.EXTERNAL_LINE_ITEM_REJECTION_3)
        .noneMatch(flag -> flag.is(lineActionFlag));
  }

  private boolean isValidPackageFlag(String packageFlag) {
    return Stream.of(
            PackageFlag.NOT_PACKAGED_0,
            PackageFlag.SERVICE_1,
            PackageFlag.PER_DIEM_2,
            PackageFlag.ARTIFICIAL_SURGICAL_3,
            PackageFlag.DRUG_ADMINISTRATION_4)
        .anyMatch(pf -> pf.is(packageFlag));
  }

  private boolean isValidAdjustmentFlag(List<String> paf) {
    return Stream.of(
            PaymentAdjustmentFlag.NO_ADJUSTMENT_0,
            PaymentAdjustmentFlag.STANDARD_1,
            PaymentAdjustmentFlag.COST_ADJUSTED_CHARGE_2,
            PaymentAdjustmentFlag.APPLIED_TO_APC_3,
            PaymentAdjustmentFlag.DEDUCTIBLE_NOT_APPLICABLE_4,
            PaymentAdjustmentFlag.BLOOD_DEDUCTIBLE_5,
            PaymentAdjustmentFlag.BLOOD_NOT_DEDUCTIBLE_6,
            PaymentAdjustmentFlag.NO_COST_7,
            PaymentAdjustmentFlag.PARTIAL_COST_8,
            PaymentAdjustmentFlag.DEDUCTIBLE_AND_COINSURANCE_NOT_APPLICABLE_9,
            PaymentAdjustmentFlag.COINSURANCE_NOT_APPLICABLE_10,
            PaymentAdjustmentFlag.MULTIPLE_SERVICE_UNITS_11,
            PaymentAdjustmentFlag.DEVICE_PASS_THROUGH_12,
            PaymentAdjustmentFlag.DEVICE_PASS_THROUGH_13,
            PaymentAdjustmentFlag.CT_SCAN_14,
            PaymentAdjustmentFlag.PLACEHOLDER_15,
            PaymentAdjustmentFlag.TERMINATED_PROCEDURE_PASS_THROUGH_DEVICE_16,
            PaymentAdjustmentFlag.DEVICE_CREDIT_17,
            PaymentAdjustmentFlag.DRUG_BIOLOGICAL_FIRST_18,
            PaymentAdjustmentFlag.DRUG_BIOLOGICAL_SECOND_19,
            PaymentAdjustmentFlag.DRUG_BIOLOGICAL_THIRD_20,
            PaymentAdjustmentFlag.X_RAY_21,
            PaymentAdjustmentFlag.COMPUTED_RADIOLOGY_22,
            PaymentAdjustmentFlag.X_RAY_NO_COINSURANCE_23,
            PaymentAdjustmentFlag.COMPUTED_RADIOLOGY_NO_COINSURANCE_24,
            PaymentAdjustmentFlag.COLONIAL_PROCEDURE_25)
        .anyMatch(si -> si.is(paf));
  }

  private boolean isValidPaymentIndicator(String paymentIndicator) {
    return Stream.of(
            PaymentIndicator.PAID_STANDARD_HOSPITAL_OPPS_AMOUNT_1,
            PaymentIndicator.PAID_STANDARD_AMOUNT_FOR_PASS_THROUGH_DRUG_OR_BIOLOGICAL_5,
            PaymentIndicator.PAYMENT_BASED_ON_CHARGE_ADJUSTED_TO_COST_6,
            PaymentIndicator.ADDITIONAL_PAYMENT_FOR_NEW_DRUG_OR_BIOLOGICAL_7,
            PaymentIndicator.PAID_PARTIAL_HOSPITALIZATION_PER_DIEM_8,
            PaymentIndicator.NO_ADDITIONAL_PAYMENT_9)
        .anyMatch(pi -> pi.is(paymentIndicator));
  }

  private boolean isValidPaymentMethodFlag(String paymentMethodFlag) {
    return Stream.of(
            PaymentMethodFlag.OPPS_0,
            PaymentMethodFlag.CMHC_LIMIT_REACHED_6,
            PaymentMethodFlag.SECTION603_NO_REDUCTION_7,
            PaymentMethodFlag.SECTION603_REDUCTION_8,
            PaymentMethodFlag.CMHC_LIMIT_BYPASSED_9,
            PaymentMethodFlag.OFF_CAMPUS_CLINIC_A,
            PaymentMethodFlag.CONTRACTOR_BYPASS_OFF_CAMPUS_CLINIC_W,
            PaymentMethodFlag.CONTRACTOR_BYPASS_SECTION_603_NO_REDUCTION_X,
            PaymentMethodFlag.CONTRACTOR_BYPASS_SECTION_603_REDUCTION_Y,
            PaymentMethodFlag.RADIATION_ONCOLOGY_MODEL_B,
            PaymentMethodFlag.CONTRACTOR_BYPASS_Z)
        .anyMatch(pmf -> pmf.is(paymentMethodFlag));
  }

  private boolean isValidStatusIndicator(String statusIndicator) {
    return Stream.of(
            StatusIndicator.A_NOT_PAID_OPPS,
            StatusIndicator.B_NOT_ALLOWED_OPPS,
            StatusIndicator.C_INPATIENT_PROCEDURE,
            StatusIndicator.E_DEPRECATED_NOT_ALLOWED,
            StatusIndicator.F_CORNEAL_TISSUE,
            StatusIndicator.G_DRUG_PASS_THROUGH,
            StatusIndicator.H_PASS_THROUGH_DEVICE,
            StatusIndicator.K_NON_PASS_THROUGH_DRUG,
            StatusIndicator.L_FLU_PPV_VACCINES,
            StatusIndicator.M_NOT_BILLABLE_TO_MAC,
            StatusIndicator.N_PACKAGED_INTO_APC,
            StatusIndicator.P_PARTIAL_HOSPITALIZATION,
            StatusIndicator.R_BLOOD,
            StatusIndicator.S_PROCEDURE_NOT_DISCOUNTED,
            StatusIndicator.T_PROCEDURE_REDUCIBLE,
            StatusIndicator.U_BRACHYTHERAPY,
            StatusIndicator.V_EMERGENCY,
            StatusIndicator.W_INVALID,
            StatusIndicator.X_ANCILLARY,
            StatusIndicator.Y_NON_IMPLANTABLE_DME,
            StatusIndicator.Z_VALID_WITH_BLANK_HCPCS,
            StatusIndicator.J1_COMPREHENSIVE_APC_OUTPATIENT,
            StatusIndicator.J2_COMPREHENSIVE_APC_HOSPITAL)
        .anyMatch(si -> si.is(statusIndicator));
  }

  private boolean isValidStatusIndicatorForOPPS(String statusIndicator) {
    return Stream.of(
            StatusIndicator.H_PASS_THROUGH_DEVICE,
            StatusIndicator.N_PACKAGED_INTO_APC,
            StatusIndicator.P_PARTIAL_HOSPITALIZATION,
            StatusIndicator.R_BLOOD,
            StatusIndicator.S_PROCEDURE_NOT_DISCOUNTED,
            StatusIndicator.T_PROCEDURE_REDUCIBLE,
            StatusIndicator.U_BRACHYTHERAPY,
            StatusIndicator.V_EMERGENCY,
            StatusIndicator.X_ANCILLARY,
            StatusIndicator.J1_COMPREHENSIVE_APC_OUTPATIENT,
            StatusIndicator.J2_COMPREHENSIVE_APC_HOSPITAL)
        .anyMatch(si -> si.is(statusIndicator));
  }

  private boolean isValidDiscountFormula(int formula) {
    return formula >= 1 && formula <= 9;
  }
}
