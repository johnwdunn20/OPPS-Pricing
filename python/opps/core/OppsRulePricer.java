package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.common.application.rules.CalculationEvaluator;
import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.RuleContextExecutor;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.tables.DataTables;
import java.util.List;

/** Base class for the business rules implementation of the OPPS pricer. */
public abstract class OppsRulePricer
    extends RuleContextExecutor<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  protected final DataTables dataTables;

  protected OppsRulePricer(
      DataTables dataTables,
      List<CalculationRule<OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext>>
          rules) {
    super(new CalculationEvaluator<>(rules));
    this.dataTables = dataTables;
  }
}
