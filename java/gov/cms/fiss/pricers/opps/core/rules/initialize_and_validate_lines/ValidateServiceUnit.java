package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;

/**
 * Service unit check.
 *
 * <p>(Extracted from 19250-CALC-DISCOUNT)
 */
public class ValidateServiceUnit extends AbstractLineCalculationRule {
  private static final String NULL_CODE = "00000";

  @Override
  public void calculate(ServiceLineContext calculationContext) {
    final IoceServiceLineData serviceLineData = calculationContext.getInput();
    final int serviceUnits = serviceLineData.getApcServiceUnits();

    // If service units is zero:
    //   if payment APC is 0000 set return code to 42
    //   otherwise set service units to 1
    if (serviceUnits == 0) {
      if (NULL_CODE.equals(serviceLineData.getPaymentApc())) {
        // Set return code for the current service line
        calculationContext.applyLineReturnCode(ReturnCode.INVALID_APC_OR_PACKAGING_FLAG_42);
      } else {
        serviceLineData.setApcServiceUnits(1);
      }
    }
  }
}
