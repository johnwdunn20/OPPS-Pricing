package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;

// STOP PROCESSING LINE IF ERROR CODE
public class CheckErrorCode
    implements CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  /**
   * Update deductible line context signifying the calculation has completed if the line has an
   * error return code.
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final ServiceLinePaymentData lineOutput =
        pricerContext.getServiceLinePaymentByLineNumber(
            calculationContext.getInput().getServiceLine().getLineNumber());
    if (Integer.parseInt(lineOutput.getReturnCode().getCode()) > 25) {
      calculationContext.setCalculationCompleted();
    }
  }
}
