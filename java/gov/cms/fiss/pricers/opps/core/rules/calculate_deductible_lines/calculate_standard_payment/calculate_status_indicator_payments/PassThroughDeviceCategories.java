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
import gov.cms.fiss.pricers.opps.core.model.DevicePassThroughInfo;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Stream;

public class PassThroughDeviceCategories extends AbstractStatusIndicatorPayments2024 {

  /** CALCULATE PAYMENT FOR SI = H (PT DEVICE) LINES. */
  @Override
  public boolean shouldExecute(DeductibleLineContext context) {
    return StatusIndicator.H_PASS_THROUGH_DEVICE.is(context.getStatusIndicator());
  }

  /**
   * Calculates payment based on status indicator.
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

    // CALCULATE PAYMENT FOR SI = H (PT DEVICE) LINES, PAYMENT IND.
    // SHOULD BE 6 FOR THESE LINES (BASED ON CHARGE ADJ. TO COST)
    // RETURN ERROR CODE 44 IF THIS IS NOT THE CASE
    if (PaymentIndicator.PAYMENT_BASED_ON_CHARGE_ADJUSTED_TO_COST_6.is(paymentIndicator)) {
      final DevicePassThroughInfo info =
          new DevicePassThroughInfo(
              pricerContext.getTotalQnPassThroughDeviceCharges(),
              pricerContext.getQnWageAdjustedPassThroughDeviceOffset(),
              pricerContext.getTotalQoPassThroughDeviceCharges(),
              pricerContext.getQoWageAdjustedPassThroughDeviceOffset());
      calculateDeviceLinePayment(pricerContext, providerData, lineCalculation, info);
      pricerContext.setBeneficiaryDeductible(
          calculateBeneficiaryDeductible(
              lineCalculation, pricerContext.getBeneficiaryDeductible()));
    } else {
      calculationContext.applyLineReturnCode(ReturnCode.INVALID_STATUS_INDICATOR_44);
    }
  }

  /**
   * CALCULATE PAYMENT FOR PAID AT COST LINES - PAYMENT BASED ON CHARGE ADJUSTED TO COST.
   *
   * <p>(19555-CALC-H-STANDARD)
   */
  protected void calculateDeviceLinePayment(
      OppsPricerContext pricerContext,
      OutpatientProviderData providerData,
      LineCalculation lineCalculation,
      DevicePassThroughInfo info) {

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    BigDecimal lineItemPayment; // T-LITEM-PYMT

    // SET LINE ITEM PAYMENT TO LINE COST - PAID AT COST
    // (IF NO DEVICE CCR USE HOSPITAL CCR)
    if (providerData.getDeviceCostToChargeRatio() == null
        || BigDecimalUtils.isZero(providerData.getDeviceCostToChargeRatio())) {

      // COMPUTE T-LITEM-PYMT ROUNDED = (H-SUB-CHRG *  L-PSF-OPCOST-RATIO)
      lineItemPayment =
          lineInput
              .getCoveredCharges()
              .multiply(providerData.getOperatingCostToChargeRatio())
              .setScale(2, RoundingMode.HALF_UP);
    } else {
      // COMPUTE T-LITEM-PYMT ROUNDED = (H-SUB-CHRG * L-PSF-DEVICE-CCR)
      lineItemPayment =
          lineInput
              .getCoveredCharges()
              .multiply(providerData.getDeviceCostToChargeRatio())
              .setScale(2, RoundingMode.HALF_UP);
    }

    lineCalculation.setPayment(lineItemPayment);

    rehFivePercentAddOn(
        pricerContext,
        providerData,
        lineCalculation,
        StatusIndicator.H_PASS_THROUGH_DEVICE.getIndicator());

    lineItemPayment = lineCalculation.getPayment();

    // FOR PASS-THROUGH DEVICE LINE WITH AN ASSOCIATED OFFSET,
    // APPLY THE OFFSET (INDICATED BY PAF = '12' OR '13' OR '15'
    // (NOT CURRENTLY USING PAF '15')
    if (isDevicePassThroughAdjustment(lineInput.getPaymentAdjustmentFlags())) {
      lineItemPayment = calculatePassThroughDeviceOffset(lineCalculation, lineItemPayment, info);
    }

    // CAPTURE PAYMENT AMOUNT
    if (BigDecimalUtils.isLessThanZero(lineItemPayment)) {
      lineCalculation.setPayment(BigDecimalUtils.ZERO);
    } else {
      lineCalculation.setPayment(lineItemPayment);
    }
  }

  private boolean isDevicePassThroughAdjustment(List<String> paymentAdjustmentFlags) {
    return Stream.of(
            PaymentAdjustmentFlag.DEVICE_PASS_THROUGH_12,
            PaymentAdjustmentFlag.DEVICE_PASS_THROUGH_13)
        .anyMatch(paf -> paf.is(paymentAdjustmentFlags));
  }

  /**
   * REDUCE LINE ITEM PAYMENTS OF PASS-THROUGH DEVICES THAT HAVE AN ASSOCIATED PROCEDURE ON THE
   * CLAIM BY THE PROCEDURE WAGE-ADJUSTED OFFSET AMOUNT.
   *
   * <p>(19556-CALC-PTD-OFFSET)
   */
  protected BigDecimal calculatePassThroughDeviceOffset(
      LineCalculation lineCalculation,
      BigDecimal lineItemPayment,
      DevicePassThroughInfo devicePassThroughInfo) {

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    final List<String> paymentAdjustmentFlags = lineInput.getPaymentAdjustmentFlags();
    final BigDecimal passThroughDeviceOffsetChargeRate; // W-PTDO-CHRG-RATE
    BigDecimal totalPassThruDeviceCharges = BigDecimalUtils.ZERO; // H-TOT-PTD-CHARGES
    BigDecimal wageAdjustedPassThruDeviceOffset = BigDecimalUtils.ZERO; // H-WA-PTD-OFFSET

    // DETERMINE WHICH VARIABLES TO USE IN CALCULATIONS
    if (PaymentAdjustmentFlag.DEVICE_PASS_THROUGH_12.is(paymentAdjustmentFlags)) {
      totalPassThruDeviceCharges = devicePassThroughInfo.getTotalQn();
      wageAdjustedPassThruDeviceOffset = devicePassThroughInfo.getWageAdjustedQn();
    } else if (PaymentAdjustmentFlag.DEVICE_PASS_THROUGH_13.is(paymentAdjustmentFlags)) {
      totalPassThruDeviceCharges = devicePassThroughInfo.getTotalQo();
      wageAdjustedPassThruDeviceOffset = devicePassThroughInfo.getWageAdjustedQo();
    }

    // DETERMINE WHAT % OF THE OFFSET WILL BE APPLIED
    if (BigDecimalUtils.isLessThanOrEqualToZero(totalPassThruDeviceCharges)) {
      return lineItemPayment;
    }

    // COMPUTE W-PTDO-CHRG-RATE ROUNDED = H-SUB-CHRG / H-TOT-PTD-CHARGES
    passThroughDeviceOffsetChargeRate =
        lineInput.getCoveredCharges().divide(totalPassThruDeviceCharges, 8, RoundingMode.HALF_UP);

    // CALCULATE THE OFFSET AMOUNT TO BE TAKEN
    // COMPUTE W-PTDO-LINE-OFFSET ROUNDED = W-PTDO-CHRG-RATE * H-WA-PTD-OFFSET
    final BigDecimal passThroughDeviceLineOffset =
        passThroughDeviceOffsetChargeRate
            .multiply(wageAdjustedPassThruDeviceOffset)
            .setScale(2, RoundingMode.HALF_UP);

    // SUBTRACT THE OFFSET AMOUNT FROM THE LINE PAYMENT
    if (BigDecimalUtils.isGreaterThanOrEqualTo(lineItemPayment, passThroughDeviceLineOffset)) {
      // COMPUTE T-LITEM-PYMT ROUNDED = T-LITEM-PYMT - W-PTDO-LINE-OFFSET
      lineItemPayment = lineItemPayment.subtract(passThroughDeviceLineOffset);
    } else {
      lineItemPayment = BigDecimalUtils.ZERO;
    }

    return lineItemPayment;
  }
}
