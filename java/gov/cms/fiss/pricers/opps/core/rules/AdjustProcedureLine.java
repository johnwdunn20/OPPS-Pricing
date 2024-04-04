package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.EvaluatingCalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import java.util.List;

public class AdjustProcedureLine
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {
  /** Contains the rule subset under AdjustProcedureLine. */
  private final EvaluatingCalculationRule<
          DeductibleLine, ServiceLinePaymentData, DeductibleLineContext>
      ruleEvaluator;

  public AdjustProcedureLine(
      List<CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext>> rules) {
    ruleEvaluator = new EvaluatingCalculationRule<>(rules);
  }

  /** Apply rule-set to each deductible line. */
  @Override
  public void calculate(OppsPricerContext context) {
    // Creates a new context for each deductible line and applies the AdjustProcedureLine rule-set
    context
        .getDeductibleLines()
        .forEach(
            line ->
                ruleEvaluator.calculate(
                    new DeductibleLineContext(context, line, context.getOutlierPaymentInfo())));
  }
}
