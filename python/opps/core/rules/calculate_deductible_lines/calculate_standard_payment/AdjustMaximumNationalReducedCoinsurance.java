package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class AdjustMaximumNationalReducedCoinsurance extends AbstractDeductibleLineRule {

  /**
   * ADJUST REDUCED COINSURANCE WHEN NECESSARY &amp; ADJUST CLAIM. TOTAL LINE ITEM REIMBURSEMENT
   * WHEN THE REDUCED AND/OR NATIONAL COINSURANCE AMOUNTS EXCEED THE MAXIMUM COINSURANCE (PROVIDER
   * MAY ELECT TO REDUCE COINSURANCE)
   *
   * <p>This rule will not execute for lines with 603 Services since rule CalculateLineReimbursement
   * marks 603 Services lines as completed.
   *
   * <p>(Extracted from 19550-CALC-STANDARD)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final LineCalculation lineCalculation = calculationContext.getLineCalculation();
    final DeductibleLine deductibleLine = lineCalculation.getDeductibleLine();

    // CALCULATE MAXIMUM COINSURANCE AMOUNT (INPATIENT LIMIT)
    // COMPUTE H-MAX-COIN ROUNDED = (H-IP-LIMIT * W-SRVC-UNITS (W-LP-INDX) * W-DISC-RATE
    // (W-LP-INDX))
    final BigDecimal maximumCoinsurance =
        calculationContext
            .getPricerContext()
            .getInpatientDeductibleLimit()
            .multiply(new BigDecimal(deductibleLine.getServiceUnits()))
            .multiply(deductibleLine.getDiscountRate())
            .setScale(2, RoundingMode.HALF_UP);

    // ADJUST REDUCED COINSURANCE WHEN NECESSARY & ADJUST CLAIM
    // TOTAL LINE ITEM REIMBURSEMENT WHEN THE REDUCED AND/OR
    // NATIONAL COINSURANCE AMOUNTS EXCEED THE MAXIMUM COINSURANCE
    // (PROVIDER MAY ELECT TO REDUCE COINSURANCE)

    // MOVE W-RED-COIN (W-LP-INDX) TO H-RED-COIN.
    //
    // IF (H-RED-COIN  > 0 AND < H-MIN-COIN)
    //    MOVE H-MIN-COIN TO H-RED-COIN
    // ELSE
    //    IF H-RED-COIN < H-NAT-COIN AND > H-MIN-COIN
    //       AND > H-MAX-COIN
    //       COMPUTE H-LITEM-REIM = H-LITEM-REIM +
    //                              (H-RED-COIN - H-MAX-COIN)
    //    END-IF
    // END-IF.
    //
    // IF H-NAT-COIN > H-MAX-COIN AND H-RED-COIN = 0 THEN
    //    COMPUTE H-LITEM-REIM = H-LITEM-REIM +
    //                           (H-NAT-COIN - H-MAX-COIN)
    //    MOVE H-MAX-COIN TO H-NAT-COIN.

    lineCalculation.setReducedCoinsurance(deductibleLine.getReducedCoinsurance());
    if (BigDecimalUtils.isGreaterThanZero(lineCalculation.getReducedCoinsurance())
        && BigDecimalUtils.isGreaterThan(
            lineCalculation.getMinimumCoinsurance(), lineCalculation.getReducedCoinsurance())) {
      lineCalculation.setReducedCoinsurance(lineCalculation.getMinimumCoinsurance());
    } else if (BigDecimalUtils.isGreaterThan(
            lineCalculation.getNationalCoinsurance(), lineCalculation.getReducedCoinsurance())
        && BigDecimalUtils.isGreaterThan(
            lineCalculation.getReducedCoinsurance(), lineCalculation.getMinimumCoinsurance())
        && BigDecimalUtils.isGreaterThan(
            lineCalculation.getReducedCoinsurance(), maximumCoinsurance)) {

      // COMPUTE H-LITEM-REIM = H-LITEM-REIM + (H-RED-COIN - H-MAX-COIN)
      lineCalculation.setReimbursement(
          lineCalculation
              .getReimbursement()
              .add(lineCalculation.getReducedCoinsurance().subtract(maximumCoinsurance)));
    }

    if (BigDecimalUtils.isGreaterThan(lineCalculation.getNationalCoinsurance(), maximumCoinsurance)
        && BigDecimalUtils.isZero(lineCalculation.getReducedCoinsurance())) {

      // COMPUTE H-LITEM-REIM = H-LITEM-REIM + (H-NAT-COIN - H-MAX-COIN)
      lineCalculation.setReimbursement(
          lineCalculation
              .getReimbursement()
              .add(lineCalculation.getNationalCoinsurance().subtract(maximumCoinsurance)));
      lineCalculation.setNationalCoinsurance(maximumCoinsurance);
    }
  }
}
