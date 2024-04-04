package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.EvaluatingCalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import java.util.List;

/**
 * STEP 1 - INITIALIZE &amp; SET WORKING-STORAGE VARIABLES, ASSIGN.
 *
 * <pre>
 *  ------   CBSA &amp; WAGE INDEX, PERFORM CLAIM DATE EDITS, SET
 *           INPATIENT DAILY COINSURANCE LIMIT (SET ANNUALLY),
 *           DETERMINE CLAIM DEVICE CREDIT AMOUNT
 *           (CLAIM LEVEL INITIALIZATION)
 *
 * INITIALIZE WORKING STORAGE HOLD AREAS AND ADDITIONAL VARIABLES TO BE PASSED BACK TO THE STANDARD
 * SYSTEM, ASSIGN CLAIM CBSA AND WAGE INDEX, &amp; PERFORM DATE EDITS
 *
 * ERROR RETURN CODES:
 * -------------------
 *    - IF PROVIDER SPECIFIC FILE WAGE INDEX RECLASSIFICATION
 *      CODE INVALID OR MISSING
 *       - MOVE '52' TO CLAIM LEVEL RETURN CODE
 *       - DISCONTINUE CLAIM PROCESSING
 *    - IF SERVICE FROM DATE NOT NUMERIC OR &lt; 20000801
 *       - MOVE '53' TO CLAIM LEVEL RETURN CODE
 *       - DISCONTINUE CLAIM PROCESSING
 *    - IF SERVICE FROM DATE &lt; PROVIDER EFFECTIVE DATE
 *       - MOVE '54' TO CLAIM LEVEL RETURN CODE
 *       - DISCONTINUE CLAIM PROCESSING
 *    - IF SERVICE FROM DATE &gt; PROVIDER TERMINATION DATE
 *       - MOVE '54' TO CLAIM LEVEL RETURN CODE
 *       - DISCONTINUE CLAIM PROCESSING
 *
 * </pre>
 *
 * <p>(19100-INIT)
 */
public class InitializeAndValidateClaim
    extends EvaluatingCalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {
  public InitializeAndValidateClaim(
      List<CalculationRule<OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext>>
          rules) {
    super(rules);
  }
}
