package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.EvaluatingCalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import java.util.Collections;
import java.util.List;

/**
 * STEP 5 - CALCULATE LINE PAYMENTS, DEDUCTIBLES, COINSURANCE &amp; REIMBURSEMENT, ACCUMULATE CLAIM
 * TOTALS, POPULATE DRUG COINSURANCE TABLE, AND MOVE LINE ITEM VALUES TO VARIABLES TO BE PASSED BACK
 * (LOOP THROUGH THE COINSURANCE DEDUCTIBLE TABLE).
 */
public class CalculateClaimLine
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {
  /** Contains the rule subset under CalculateClaimLine. */
  private final EvaluatingCalculationRule<
          DeductibleLine, ServiceLinePaymentData, DeductibleLineContext>
      ruleEvaluator;

  public CalculateClaimLine(
      List<CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext>> rules) {
    ruleEvaluator = new EvaluatingCalculationRule<>(rules);
  }

  /** Apply rule-set to each deductible line. */
  @Override
  public void calculate(OppsPricerContext context) {
    // Creates a new context for each deductible line and applies the CalculateClaimLine rule-set
    context
        .getDeductibleLines()
        .forEach(line -> ruleEvaluator.calculate(new DeductibleLineContext(context, line)));

    // Sort coinsurance cap table by date of service followed by code
    Collections.sort(context.getCoinsuranceCaps());
  }
}
