package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;

public abstract class AbstractLineCalculationRule
    implements CalculationRule<IoceServiceLineData, ServiceLinePaymentData, ServiceLineContext> {

  /** Rule only applies to lines without an error code. */
  @Override
  public boolean shouldExecute(ServiceLineContext calculationContext) {
    return Integer.parseInt(calculationContext.getOutput().getReturnCode().getCode()) <= 25;
  }
}
