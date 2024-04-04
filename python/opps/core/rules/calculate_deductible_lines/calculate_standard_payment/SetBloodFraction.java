package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;

public class SetBloodFraction extends AbstractDeductibleLineRule {
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    // INITIALIZE VARIABLE FOR LINES ELIGIBLE FOR BLOOD DEDUCTIBLE
    calculationContext.getLineCalculation().setBloodFraction(BigDecimalUtils.ZERO);
  }
}
