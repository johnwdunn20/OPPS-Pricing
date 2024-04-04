package gov.cms.fiss.pricers.opps.core.rules.adjust_procedure_lines;

import gov.cms.fiss.pricers.common.api.OutpatientProviderData;
import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.OutlierPaymentInfo;
import gov.cms.fiss.pricers.opps.core.codes.PaymentMethodFlag;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

public class CalculateOutlierPayment
    implements CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  @Override
  public boolean shouldExecute(DeductibleLineContext calculationContext) {
    // Skip this rule if line payment has not been set
    return calculationContext.getLineCalculation().getPayment() != null;
  }

  /**
   * CALCULATE THE OUTLIER PAYMENT FOR ELIGIBLE LINES.
   *
   * <p>(Extracted from 19600-ADJ-CHRG-OUTL)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final DeductibleLine deductibleLine = calculationContext.getInput();
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final OutlierPaymentInfo outlierPaymentInfo = calculationContext.getOutlierPaymentInfo();
    final OutpatientProviderData providerData = pricerContext.getProviderData();
    BigDecimal outlierPayment = outlierPaymentInfo.getOutlierPayment();
    final IoceServiceLineData line = deductibleLine.getServiceLine();
    final BigDecimal linePayment = calculationContext.getLineCalculation().getPayment();

    // COMPUTE H-COST ROUNDED = W-SUB-CHRG (W-LP-INDX) * L-PSF-OPCOST-RATIO
    final BigDecimal cost =
        deductibleLine
            .getSubCharge()
            .multiply(providerData.getOperatingCostToChargeRatio())
            .setScale(2, RoundingMode.HALF_UP);

    // H-LITEM-OUTL-PYMT
    BigDecimal lineOutlierPayment;

    // IDENTIFY COMMUNITY MENTAL HEALTH CENTER (CMHC) PROVIDERS FOR CMHCS, INCREASE OUTLIER FACTOR &
    // CALC LINE OUTLIER PMT (CMHC PROVIDERS ONLY PROCESS PARTIAL HOSPITALIZATION LINES)
    if (pricerContext.isCommunityMentalHealthCenter()) {

      // COMPUTE H-LITEM-OUTL-PYMT ROUNDED = (H-COST - (H-OUTLIER-FACTOR * H-LITEM-PYMT-OUTL)) *
      // H-OUTLIER-PCT
      lineOutlierPayment =
          cost.subtract(OppsPricerContext.OUTLIER_FACTOR_ALT.multiply(linePayment))
              .multiply(OppsPricerContext.OUTLIER_PERCENT)
              .setScale(2, RoundingMode.HALF_UP);

      // ADD LOGIC TO PREVENT NEGATIVE OUTLIER PAYMENT
      if (BigDecimalUtils.isLessThanZero(lineOutlierPayment)) {
        lineOutlierPayment = BigDecimal.ZERO;
      }

      // FOR CMHC PROVIDERS THAT ARE SUBJECT TO THE OUTLIER CAP, ACCUMULATE CLAIM PAYMENT AND
      // OUTLIER TOTALS
      if (isSubjectToOutlierCap(line.getPaymentMethodFlag())) {

        // COMPUTE H-CMHC-PYMT-TOTAL = H-CMHC-PYMT-TOTAL + H-LITEM-PYMT-OUTL
        outlierPaymentInfo.setCmhcTotalPayment(
            outlierPaymentInfo.getCmhcTotalPayment().add(linePayment));

        // COMPUTE H-CMHC-OUTL-TOTAL = H-CMHC-OUTL-TOTAL + H-LITEM-OUTL-PYMT
        outlierPaymentInfo.setCmhcTotalOutlier(
            outlierPaymentInfo.getCmhcTotalOutlier().add(lineOutlierPayment));
      }
    }
    // FOR NON-CMHC PROVIDERS, LINE'S OUTLIER ELIGIBILITY & CALCULATE OUTLIER PAYMENT IF ELIGIBLE
    else {
      // COMPUTE H-APC-ADJ-PYMT ROUNDED = H-OUTLIER-FACTOR * H-LITEM-PYMT-OUTL
      final BigDecimal apcAdjPymt =
          OppsPricerContext.OUTLIER_FACTOR.multiply(linePayment).setScale(2, RoundingMode.HALF_UP);

      if (BigDecimalUtils.isGreaterThan(cost, apcAdjPymt)
          && BigDecimalUtils.isGreaterThan(
              cost, linePayment.add(pricerContext.getLinePaymentOutlierOffset()))) {
        // COMPUTE H-LITEM-OUTL-PYMT ROUNDED = (H-COST - H-APC-ADJ-PYMT) * H-OUTLIER-PCT
        lineOutlierPayment =
            cost.subtract(apcAdjPymt)
                .multiply(OppsPricerContext.OUTLIER_PERCENT)
                .setScale(2, RoundingMode.HALF_UP);
      } else {
        lineOutlierPayment = BigDecimalUtils.ZERO;
      }
    }

    // ACCUMULATE TOTAL CLAIM OUTLIER PAYMENTS
    if (BigDecimalUtils.isGreaterThanZero(lineOutlierPayment)) {

      // COMPUTE H-OUTLIER-PYMT = H-OUTLIER-PYMT + H-LITEM-OUTL-PYMT
      outlierPayment = outlierPayment.add(lineOutlierPayment);

      // LINES THAT RECEIVE AN EXTERNAL ADJUSTMENT ARE NOT ELIGIBLE FOR AN OUTLIER PAYMENT - ZERO
      // OUT PAYMENT & REMOVE FROM CLAIM TOTAL
      if (OppsPricerContext.hasExternalAdjustment(line.getActionFlag())) {

        // COMPUTE H-OUTLIER-PYMT = H-OUTLIER-PYMT - H-LITEM-OUTL-PYMT
        outlierPayment = outlierPayment.subtract(lineOutlierPayment);
        lineOutlierPayment = BigDecimalUtils.ZERO;
      }
    }

    // LINES THAT ARE NOT ELIGIBLE FOR AN OUTLIER PAYMENT BECAUSE OUTLIER CAP WAS MET BEFORE THIS
    // CLAIM WAS PROCESSED - ZERO OUT LINE OUTLIER PAYMENT & REMOVE FROM CLAIM TOTAL
    if (!isEligibleForAnOutlierPayment(line.getPaymentMethodFlag())
        && BigDecimalUtils.isGreaterThanZero(lineOutlierPayment)) {

      // COMPUTE H-OUTLIER-PYMT = H-OUTLIER-PYMT - H-LITEM-OUTL-PYMT
      outlierPayment = outlierPayment.subtract(lineOutlierPayment);
      pricerContext.applyClaimReturnCode(ReturnCode.CMHC_LIMIT_REACHED_2);
    }

    outlierPaymentInfo.setOutlierPayment(outlierPayment);
  }

  /**
   * Determines if a line is subject to the outlier cap.
   *
   * @param paymentMethodFlag the line flag to check
   * @return {@code true} if the payment is subject to the cap; {@code false} otherwise
   */
  private boolean isSubjectToOutlierCap(String paymentMethodFlag) {
    return Stream.of(PaymentMethodFlag.OPPS_0, PaymentMethodFlag.CONTRACTOR_BYPASS_Z)
        .anyMatch(paf -> paf.is(paymentMethodFlag));
  }

  /**
   * Determines if a line is eligible for an outlier payment.
   *
   * @param paymentMethodFlag the line flag to check
   * @return {@code true} if the line is eligible for an outlier payment; {@code false} otherwise
   */
  private boolean isEligibleForAnOutlierPayment(String paymentMethodFlag) {
    // Radiation Oncology Model (RO) claims not eligible for Outlier payment.
    return Stream.of(
            PaymentMethodFlag.CMHC_LIMIT_REACHED_6, PaymentMethodFlag.RADIATION_ONCOLOGY_MODEL_B)
        .noneMatch(paf -> paf.is(paymentMethodFlag));
  }
}
