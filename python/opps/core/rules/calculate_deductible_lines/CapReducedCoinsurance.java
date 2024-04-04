package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;

public class CapReducedCoinsurance
    implements CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  /**
   * Caps reduced coinsurance amount to the national coinsurance amount.
   *
   * <p>(Extracted from 19400-CALCULATE)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final LineCalculation lineCalculation = calculationContext.getLineCalculation();

    // IF H-RED-COIN > H-NAT-COIN
    //   MOVE H-NAT-COIN TO A-RED-COIN (LN-SUB)
    // END-IF

    if (BigDecimalUtils.isGreaterThan(
        lineCalculation.getReducedCoinsurance(), lineCalculation.getNationalCoinsurance())) {
      calculationContext
          .getOutput()
          .setReducedCoinsurance(calculationContext.getLineCalculation().getNationalCoinsurance());
    }

    // Rule AccumulateClaimTotals conditionally updates H-TOT-PYMT based on action flag making the
    // following unnecessary

    // IF OPPS-LITEM-ACT-FLAG (LN-SUB) = '4'
    //     MOVE 0 TO A-LITEM-PYMT (LN-SUB)
    //     MOVE 0 TO A-LITEM-REIM (LN-SUB)
    //     COMPUTE H-TOT-PYMT = H-TOT-PYMT - H-LITEM-PYMT
    // END-IF
  }
}
