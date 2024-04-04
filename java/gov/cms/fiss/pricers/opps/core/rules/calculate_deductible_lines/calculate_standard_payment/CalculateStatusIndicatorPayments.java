package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.EvaluatingCalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import java.util.List;

/** Evaluating calculation rule for running a set of sub rules. */
public class CalculateStatusIndicatorPayments
    extends EvaluatingCalculationRule<
        DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {
  public CalculateStatusIndicatorPayments(
      List<CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext>>
          calculationRules) {
    super(calculationRules);
  }

  /** Determines if the sub-rules should be executed. */
  @Override
  public boolean shouldExecute(DeductibleLineContext calculationContext) {
    return !calculationContext.isStandardPaymentCalculationCompleted();
  }
}
