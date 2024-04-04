package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.OutlierPaymentInfo;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class EnforceCmhcOutlierPaymentCaps
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  /**
   * CAP CMHC TOTAL OUTLIER PAYMENTS.
   *
   * <pre>
   * FOR CMHC CLAIMS ONLY, DO THE FOLLOWING:
   *
   * - DETERMINE IF THE TOTAL CLAIM OUTLIER PAYMENT ELIGIBLE
   *   FOR CAPPING IS &gt; $0
   * - CALCULATE THE CMHC'S TOTAL CY OPPS PAYMENTS INCLUDING THE
   *   CURRENT CLAIM'S PAYMENTS
   * - CALCULATE THE CMHC'S TOTAL CY OPPS OUTLIER PAYMENTS
   *   INCLUDING THE CURRENT CLAIM'S OUTLIER PAYMENTS
   * - CALCULATE THE CURRENT OUTLIER PERCET
   * - IF THE OUTLIER PERCENT EXCEEDS THE CAP:
   *   - SET THE CLAIM OUTLIER TO $0
   *   - SET THE RETURN CODE TO 02
   * </pre>
   *
   * <p>(19610-CMHC-OUTL-CAP)
   */
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final OppsClaimPricingRequest input = calculationContext.getInput();
    final OutlierPaymentInfo outlierPaymentInfo = calculationContext.getOutlierPaymentInfo();
    BigDecimal cmhcTotalOutlier = outlierPaymentInfo.getCmhcTotalOutlier();
    BigDecimal cmhcTotalPayment = outlierPaymentInfo.getCmhcTotalPayment();
    BigDecimal outlierPayment = outlierPaymentInfo.getOutlierPayment();

    if (calculationContext.isCommunityMentalHealthCenter()
        && BigDecimalUtils.isGreaterThanZero(cmhcTotalOutlier)) {

      // CALCULATE PROVIDER'S TOTAL PAYMENTS
      // COMPUTE H-CMHC-PYMT-TOTAL =  H-CMHC-PYMT-TOTAL + L-PRIOR-PYMT-TOTAL
      cmhcTotalPayment = cmhcTotalPayment.add(input.getClaimData().getPriorPaymentTotal());

      // CALCULATE PROVIDER'S TOTAL OUTLIER PAYMENTS
      // COMPUTE H-CMHC-OUTL-TOTAL =  H-CMHC-OUTL-TOTAL + L-PRIOR-OUTL-TOTAL
      cmhcTotalOutlier = cmhcTotalOutlier.add(input.getClaimData().getPriorOutlierTotal());

      // APPLY OUTLIER CAP
      // COMPUTE H-CMHC-OUTLIER-PCT ROUNDED = H-CMHC-OUTL-TOTAL / H-CMHC-PYMT-TOTAL
      if (BigDecimalUtils.isGreaterThan(
          cmhcTotalOutlier.divide(cmhcTotalPayment, RoundingMode.HALF_UP),
          OppsPricerContext.CMHC_OUTLIER_PERCENT_CAP)) {
        outlierPayment = BigDecimalUtils.ZERO;
        calculationContext.applyClaimReturnCode(ReturnCode.CMHC_LIMIT_REACHED_2);
      }
    }

    calculationContext.setOutlierPaymentInfo(
        new OutlierPaymentInfo(cmhcTotalPayment, cmhcTotalOutlier, outlierPayment));
  }
}
