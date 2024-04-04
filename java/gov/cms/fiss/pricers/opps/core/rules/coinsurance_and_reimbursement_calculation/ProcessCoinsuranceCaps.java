package gov.cms.fiss.pricers.opps.core.rules.coinsurance_and_reimbursement_calculation;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.EvaluatingCalculationRule;
import gov.cms.fiss.pricers.opps.core.CoinsuranceCapContext;
import gov.cms.fiss.pricers.opps.core.model.CoinsuranceCapEntry;
import gov.cms.fiss.pricers.opps.core.model.CoinsuranceCapValues;
import java.util.List;

/**
 * PROCESS COINSURANCE CAP ROLL-UP TABLE RECORDS
 *
 * <pre>
 * ADJUST THE BLOOD LINE COINSURANCE WHEN THE PROCEDURE
 * COINSURANCE AMOUNTS PLUS THE BLOOD COINSURANCE AMOUNT(S)
 * BILLED ON THE SAME DAY EXCEED THE DAILY INPATIENT COINSURANCE LIMIT.
 * </pre>
 *
 * <p>(19800-ADJ-STV-REIM)
 */
public class ProcessCoinsuranceCaps
    extends EvaluatingCalculationRule<
        CoinsuranceCapEntry, CoinsuranceCapValues, CoinsuranceCapContext> {

  public ProcessCoinsuranceCaps(
      List<CalculationRule<CoinsuranceCapEntry, CoinsuranceCapValues, CoinsuranceCapContext>>
          rules) {
    super(rules);
  }
}
