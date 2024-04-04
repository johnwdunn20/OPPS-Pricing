package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.EvaluatingCalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import java.util.List;

/**
 * STEP 3 - VALIDATE CLAIM LINES, CALCULATE LINE DISCOUNTS
 *
 * <pre>
 *    ------   OFFSETS, ACCUMULATE CLAIM TOTALS, SET FLAGS,
 *             POPULATE COINSURANCE &amp; BLOOD DEDUCTIBLE TABLES
 *             WITH VALID SERVICE LINES, POPULATE COMPOSITE APC
 *             TABLE WITH NON-PRIME COMPOSITE LINE CHARGES,
 *             CREATE NUCLEAR MEDICINE APC TABLE FOR PASS-THROUGH
 *             RADIOPHARMACEUTICAL OFFSET, CREATE CONTRAST
 *             AGENT PROCEDURE TABLE FOR PASS-THROUGH CONTRAST
 *             AGENT OFFSET.
 *             (LOOP THROUGH THE CLAIM
 *             LINES)
 *
 * VALIDATE CLAIM LINES, CALCULATE LINE DISCOUNTS &amp; OFFSETS, ACCUMULATE CLAIM TOTALS, SET FLAGS,
 * POPULATE COINSURANCE &amp; BLOOD DEDUCTIBLE TABLES WITH VALID SERVICE LINES, POPULATE COMPOSITE
 * APC TABLE WITH NON-PRIME COMPOSITE LINE CHARGES
 *
 * VALIDATION RULES &amp; RETURN CODES:
 * --------------------------------
 *
 * 1. VERIFY THE SERVICE INDICATOR PASSED BY THE OCE
 *      - IF INVALID SET RETURN CODE TO '40'
 *         - DISCONTINUE LINE PROCESSING
 *      - IF VALID SET RETURN CODE TO '01'
 *         - CONTINUE LINE PROCESSING
 * 2. PROCESS LINES AND LOAD WORK TABLE ACCORDING TO PRICE
 *    RANKING
 * 3. CHECK OCE EDIT INDICATORS AND SET RETURN CODES IF
 *    INDICATORS ARE INVALID.
 *     - VALID RETURN CODES FOR EDIT INDICATORS
 *       - '41' - SERVICE INDICATOR INVALID FOR OPPS PRICER
 *       - '42' - APC = '00000' OR (PACKAGING FLAG = 1, 2,OR 4)
 *       - '43' - PAYMENT INDICATOR NOT = TO 1 OR 5 THRU 9
 *       - '44' - SERVICE INDICATOR = 'H' BUT PAYMENT
 *                  INDICATOR NOT = TO 6
 *       - '45' - PACKAGING FLAG NOT = TO 0, 1, 2, 3, OR 4
 *       - '46' - (LINE ITEM DENIAL/REJECT FLAG NOT = TO 0
 *                 OR LINE ITEM ACTION FLAG NOT = TO 1
 *       - '47' - LINE ITEM ACTION FLAG = 2 OR 3
 *       - '48' - PAYMENT ADJUSTMENT FLAG NOT VALID
 *       - '49' - SITE OF SERVICE FLAG NOT = TO 0, 6, 7, 8,
 *                9, OR 'A' (PAYMENT METHOD FLAG)
 * 4. IF OCE INDICATORS ARE VALID, SEARCH APC TABLE
 *     - IF MISSING, DELETED OR INVALID APC
 *       - SET RETURN CODE TO '30'
 *         - DISCONTINUE LINE PROCESSING
 * </pre>
 *
 * <p>(19150-INIT)
 */
public class InitializeAndValidateLines
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {
  /** Contains the rule subset under InitializeAndValidateLines. */
  private final EvaluatingCalculationRule<
          IoceServiceLineData, ServiceLinePaymentData, ServiceLineContext>
      ruleEvaluator;

  public InitializeAndValidateLines(
      List<CalculationRule<IoceServiceLineData, ServiceLinePaymentData, ServiceLineContext>>
          rules) {
    ruleEvaluator = new EvaluatingCalculationRule<>(rules);
  }

  /** Apply rule-set to each service line. */
  @Override
  public void calculate(OppsPricerContext context) {
    final List<IoceServiceLineData> lines = context.getClaimData().getIoceServiceLines();
    for (final IoceServiceLineData serviceLine : lines) {

      // Update null HCPCS codes to empty string
      if (serviceLine.getHcpcsCode() == null) {
        serviceLine.setHcpcsCode("");
      }

      // Retrieve the output line that corresponds to the current service line number
      final ServiceLinePaymentData serviceLineOutput =
          context.getServiceLinePaymentByLineNumber(serviceLine.getLineNumber());

      // Create a service line context for the current line
      final ServiceLineContext serviceLineContext =
          new ServiceLineContext(context, serviceLine, serviceLineOutput);

      // Initialize line status
      serviceLineContext.applyLineReturnCode(ReturnCode.PROCESSED_1);

      // Apply rule-set to current line
      ruleEvaluator.calculate(serviceLineContext);
    }
  }
}
