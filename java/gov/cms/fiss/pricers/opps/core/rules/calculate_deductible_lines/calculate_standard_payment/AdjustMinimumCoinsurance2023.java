package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public class AdjustMinimumCoinsurance2023 extends AbstractDeductibleLineRule {

  /**
   * ADJUST MINIMUM COINSURANCE AMOUNT.
   *
   * <p>(Extracted from 19550-CALC-STANDARD)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final LineCalculation lineCalculation = calculationContext.getLineCalculation();

    // ADJUST MINIMUM COINSURANCE AMOUNT
    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    final DeductibleLine deductibleLine = lineCalculation.getDeductibleLine();

    lineCalculation.setMinimumCoinsurance(deductibleLine.getMinimumCoinsurance());

    if (BigDecimalUtils.isGreaterThanZero(deductibleLine.getMinimumCoinsurance())) {
      // DEVICES, BRACHYTHERAPY, & BLOOD (SI = H LINES HAVE A MIN-COIN = 0, WON'T ENTER LOGIC)
      if (isPassThroughDeviceOrBloodProductOrBrachytherapy(lineInput.getStatusIndicator())) {

        // COMPUTE H-MIN-COIN ROUNDED =
        //                     H-MIN-COIN * (W-SRVC-UNITS (W-LP-INDX) -
        //                     (W-SRVC-UNITS (W-LP-INDX) * H-BLOOD-FRACTION)) *
        //                     W-DISC-RATE (W-LP-INDX)
        lineCalculation.setMinimumCoinsurance(
            lineCalculation
                .getMinimumCoinsurance()
                .multiply(
                    new BigDecimal(deductibleLine.getServiceUnits())
                        .subtract(
                            new BigDecimal(deductibleLine.getServiceUnits())
                                .multiply(lineCalculation.getBloodFraction())))
                .multiply(deductibleLine.getDiscountRate())
                .setScale(2, RoundingMode.HALF_UP));
      }
      // APCS 0158 & 0159'S MIN COINSURANCE = 25% OF LINE PMT
      // Last APC update was in 2015, Colorectal Cancer Screening APCs
      else if (isColorectalCancerScreening(lineInput.getPaymentApc())) {

        // check if REH Provider Type  = '24', subtract 5% add on from payment
        if (calculationContext.getPricerContext().isRehProviderType24Or25()) {
          // COMPUTE H-MIN-COIN ROUNDED = H-LITEM-PYMT * .25
          lineCalculation.setMinimumCoinsurance(
              lineCalculation
                  .getPayment()
                  .subtract(calculationContext.getPricerContext().getRehFivePercentAddon())
                  .multiply(new BigDecimal(".25"))
                  .setScale(2, RoundingMode.HALF_UP));
        } else {
          // COMPUTE H-MIN-COIN ROUNDED = H-LITEM-PYMT * .25
          lineCalculation.setMinimumCoinsurance(
              lineCalculation
                  .getPayment()
                  .multiply(new BigDecimal(".25"))
                  .setScale(2, RoundingMode.HALF_UP));
        }
      }
      // OTHER SERVICE TYPES AND APCS' MIN COIN = 20% OF LINE PMT
      else {
        // check if REH Provider Type  = '24', subtract 5% add on from payment
        if (calculationContext.getPricerContext().isRehProviderType24Or25()) {
          // COMPUTE H-MIN-COIN ROUNDED = H-LITEM-PYMT * .2
          lineCalculation.setMinimumCoinsurance(
              lineCalculation
                  .getPayment()
                  .subtract(calculationContext.getPricerContext().getRehFivePercentAddon())
                  .multiply(new BigDecimal(".20"))
                  .setScale(2, RoundingMode.HALF_UP));
        } else {
          // COMPUTE H-MIN-COIN ROUNDED = H-LITEM-PYMT * .2
          lineCalculation.setMinimumCoinsurance(
              lineCalculation
                  .getPayment()
                  .multiply(new BigDecimal(".20"))
                  .setScale(2, RoundingMode.HALF_UP));
        }
      }
    }
  }

  private boolean isPassThroughDeviceOrBloodProductOrBrachytherapy(String statusIndicator) {
    return Stream.of(
            StatusIndicator.H_PASS_THROUGH_DEVICE,
            StatusIndicator.R_BLOOD,
            StatusIndicator.U_BRACHYTHERAPY)
        .anyMatch(si -> si.is(statusIndicator));
  }

  private boolean isColorectalCancerScreening(String apc) {
    return StringUtils.equalsAny(apc, "00158", "00159");
  }
}
