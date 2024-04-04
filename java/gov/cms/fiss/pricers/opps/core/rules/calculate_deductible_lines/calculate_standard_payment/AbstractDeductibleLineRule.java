package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;

public abstract class AbstractDeductibleLineRule
    implements CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  /** Rule only applies to lines without an error code. */
  @Override
  public boolean shouldExecute(DeductibleLineContext calculationContext) {
    return !calculationContext.isStandardPaymentCalculationCompleted();
  }
}
