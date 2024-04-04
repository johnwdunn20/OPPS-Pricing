package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.EvaluatingCalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.CoinsuranceCapContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.model.CoinsuranceCapEntry;
import gov.cms.fiss.pricers.opps.core.model.CoinsuranceCapValues;
import java.util.List;

public class ApplyDailyInpatientCoinsuranceCap
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {
  /** Contains the rule subset under ApplyDailyInpatientCoinsuranceCap. */
  EvaluatingCalculationRule<CoinsuranceCapEntry, CoinsuranceCapValues, CoinsuranceCapContext>
      ruleEvaluator;

  public ApplyDailyInpatientCoinsuranceCap(
      List<CalculationRule<CoinsuranceCapEntry, CoinsuranceCapValues, CoinsuranceCapContext>>
          rules) {
    ruleEvaluator = new EvaluatingCalculationRule<>(rules);
  }

  @Override
  public boolean shouldExecute(OppsPricerContext calculationContext) {
    return calculationContext.isBloodFlag();
  }

  /** Apply rule-set to each coinsurance cap entry. */
  @Override
  public void calculate(OppsPricerContext context) {
    final CoinsuranceCapValues capValues = new CoinsuranceCapValues();

    context
        .getCoinsuranceCaps()
        .forEach(
            entry -> ruleEvaluator.calculate(new CoinsuranceCapContext(entry, capValues, context)));
  }
}
