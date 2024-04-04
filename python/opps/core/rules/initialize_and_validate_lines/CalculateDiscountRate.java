package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculateDiscountRate extends AbstractLineCalculationRule {

  @Override
  public void calculate(ServiceLineContext calculationContext) {
    // CALCULATE DISCOUNT RATE (FOR REDUCED COIN & OFFSET CALCS)
    calculationContext
        .getPricerContext()
        .setDiscountRate(calculateDiscountRate(calculationContext.getInput()));
  }

  /**
   * CALCULATE DISCOUNT RATE BASED ON THE DISCOUNT FACTOR PASSED BY THE OCE: VALUES 1 - 9.
   *
   * <pre>
   * IF MISSING OR INVALID DISCOUNT FACTOR
   *  - SET RETURN CODE TO '38'
   *  - DISCONTINUE LINE PROCESSING
   * </pre>
   *
   * <p>(19250-CALC-DISCOUNT)
   */
  protected BigDecimal calculateDiscountRate(IoceServiceLineData ioceServiceLine) {
    // CALCULATE DISCOUNT RATE (FOR REDUCED COIN & OFFSET CALCS)
    final int serviceUnits = ioceServiceLine.getApcServiceUnits();

    switch (ioceServiceLine.getDiscountingFormula()) {
      case 1:
        return BigDecimal.ONE;
      case 2:
        final int minusOne = serviceUnits - 1;
        // COMPUTE H-DISC-RATE = (1 + DISC-FRACTION  * (H-SRVC-UNITS - 1)) / H-SRVC-UNITS
        return OppsPricerContext.DISCOUNT_FRACTION
            .multiply(new BigDecimal(minusOne))
            .add(BigDecimal.ONE)
            .divide(new BigDecimal(serviceUnits), 8, RoundingMode.DOWN);
      case 3:
        // COMPUTE H-DISC-RATE = TERM-PROC-DISC / H-SRVC-UNITS
        return OppsPricerContext.TERMINATION_PROCEDURE_DISCOUNT.divide(
            new BigDecimal(serviceUnits), 8, RoundingMode.DOWN);
      case 4:
        // COMPUTE H-DISC-RATE = (1 + DISC-FRACTION) / H-SRVC-UNITS
        return OppsPricerContext.DISCOUNT_FRACTION
            .add(BigDecimal.ONE)
            .divide(new BigDecimal(serviceUnits), 8, RoundingMode.DOWN);
      case 5:
        // COMPUTE H-DISC-RATE = DISC-FRACTION
        return OppsPricerContext.DISCOUNT_FRACTION;
      case 6:
        // COMPUTE H-DISC-RATE = (TERM-PROC-DISC * DISC-FRACTION) / H-SRVC-UNITS
        return OppsPricerContext.TERMINATION_PROCEDURE_DISCOUNT
            .multiply(OppsPricerContext.DISCOUNT_FRACTION)
            .divide(new BigDecimal(serviceUnits), 8, RoundingMode.DOWN);
      case 7:
        // COMPUTE H-DISC-RATE = (DISC-FRACTION * (1 + DISC-FRACTION) / H-SRVC-UNITS)
        return OppsPricerContext.DISCOUNT_FRACTION
            .add(BigDecimal.ONE)
            .multiply(OppsPricerContext.DISCOUNT_FRACTION)
            .divide(new BigDecimal(serviceUnits), 8, RoundingMode.DOWN);
      case 8:
        return new BigDecimal("2.00000000");
      case 9:
        // COMPUTE H-DISC-RATE = 1 / H-SRVC-UNITS
        return BigDecimal.ONE.divide(new BigDecimal(serviceUnits), 8, RoundingMode.DOWN);
      default:
        return BigDecimalUtils.ZERO;
        // ReturnCode.DISCOUNT_FACTOR_INDICATOR_INVALID_38
        // COBOL code sets line RTC to 38 for an invalid value however the line RTC is
        // subsequently overwritten with 40 or 01.
    }
  }
}
