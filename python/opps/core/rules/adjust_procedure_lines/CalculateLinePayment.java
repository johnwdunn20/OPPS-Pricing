package gov.cms.fiss.pricers.opps.core.rules.adjust_procedure_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.PackageFlag;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

public class CalculateLinePayment
    implements CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  /**
   * CALCULATE OUTLIER PAYMENT.
   *
   * <pre>
   * LOOP THROUGH THE COINSURANCE DEDUCTIBLE TABLE TO DO THE
   * FOLLOWING FOR EACH SERVICE LINE:
   *
   * - DETERMINE IF THE LINE IS ELIGIBLE FOR AN OUTLIER PAYMENT
   * - ADJUST CHARGES FOR LINES WITH ARTIFICIAL CHARGES ON CLAIM
   * - DISTRIBUTE TOTAL CLAIM PACKAGED LINE CHARGES TO NON-
   *   PACKAGED PAYABLE LINES
   * - ADJUST CHARGES OF PRIME CODE COMPOSITE APC LINES
   * - ADJUST CHARGES OF MENTAL HEALTH LINES (APC = 34)
   * - CALCULATE THE LINE OUTLIER PAYMENT FOR ELIGIBLE LINES
   * </pre>
   *
   * <p>(19600-ADJ-CHRG-OUTL)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final DeductibleLine deductibleLine = calculationContext.getInput();
    final IoceServiceLineData line = deductibleLine.getServiceLine();
    final String statusIndicator = line.getStatusIndicator();

    // SERVICE LINES NOT ELIGIBLE FOR AN OUTLIER PAYMENT:
    //  - DEVICES, PACKAGED, PACKAGED AS PART OF DRUG ADMIN, AND SECTION 603 SERVICES
    // BLOOD LINES ELIGIBLE FOR THE BLOOD DEDUCTIBLE AND ON A CLAIM WITH A COMPREHENSIVE APC NOT
    // ELIGIBLE FOR OUTLIER
    if (pricerContext.notEligibleForOutlierPayment(
        statusIndicator,
        line.getPackageFlag(),
        line.getPaymentMethodFlag(),
        line.getPaymentAdjustmentFlags())) {
      calculationContext.getLineCalculation().setPayment(null);
      return;
    }

    // GO TO SERVICE LINE ASSOCIATED WITH THE CURRENT COINSURANCE DEDUCTIBLE TABLE RECORD
    final BigDecimal linePayment =
        pricerContext.getServiceLinePaymentByLineNumber(line.getLineNumber()).getPayment();

    // DISTRIBUTE TOTAL CLAIM PROCEDURE CHARGES TO PROCEDURE LINES IN PROPORTION TO THE PROCEDURE
    // LINES' PAYMENTS WHEN THERE ARE ARTIFICIAL PROCEDURE CHARGES (< $1.01) ON THE CLAIM.
    // (DISTRIBUTED CHARGES REPLACE BILLED CHARGES)
    //
    // NOTE: LINES WITH CHARGES < $1.01 ARE FIXED BY FISS BEFORE ENTERING THE PRICER.  THEREFORE,
    // THIS LOGIC IS NO LONGER NECESSARY.  THIS LOGIC WAS NECESSARY BEFORE FISS IMPLEMENTED THE FIX.
    if (pricerContext.isSt0Flag()
        && OppsPricerContext.isSignificantProcedure(statusIndicator, line.getHcpcsCode())
        && BigDecimalUtils.isGreaterThanZero(pricerContext.getTotalSTPayment())) {

      // COMPUTE H-CHRG-RATE ROUNDED = (A-LITEM-PYMT (LN-SUB) / H-TOT-ST-PYMT)
      // COMPUTE W-SUB-CHRG (W-LP-INDX) ROUNDED = (H-CHRG-RATE * H-TOT-ST-CHRG)
      deductibleLine.setSubCharge(
          linePayment
              .divide(pricerContext.getTotalSTPayment(), 8, RoundingMode.HALF_UP)
              .multiply(pricerContext.getTotalSTCharge())
              .setScale(2, RoundingMode.HALF_UP));
    }

    // DISTRIBUTE TOTAL CLAIM PACKAGED LINE CHARGES TO SEPARATELY PAYABLE LINES IN PROPORTION TO
    // THEIR PAYMENTS WHEN THERE ARE NO ARTIFICIAL PROCEDURE CHARGES (< $1.01) ON THE CLAIM.
    // (DISTRIBUTED CHARGES ARE ADDED TO THE BILLED CHARGES)
    else if (pricerContext.isNFlag()
        && !pricerContext.isSt0Flag()
        && OppsPricerContext.isSeparatelyPayable(statusIndicator)
        && isArtificialOrNotPackaged(line.getPackageFlag())
        && BigDecimalUtils.isGreaterThanZero(pricerContext.getTotalSTVXPayment())) {

      // COMPUTE H-CHRG-RATE ROUNDED = (A-LITEM-PYMT (LN-SUB) / H-TOT-STVX-PYMT)
      // COMPUTE H-SUB-CHRG ROUNDED = (H-CHRG-RATE * H-TOT-N-CHRG)
      // COMPUTE W-SUB-CHRG (W-LP-INDX) ROUNDED = W-SUB-CHRG (W-LP-INDX) + H-SUB-CHRG
      deductibleLine.setSubCharge(
          linePayment
              .divide(pricerContext.getTotalSTVXPayment(), 8, RoundingMode.HALF_UP)
              .multiply(pricerContext.getTotalNCharge())
              .setScale(2, RoundingMode.HALF_UP)
              .add(deductibleLine.getSubCharge()));
    }

    // DISTRIBUTE TOTAL CLAIM PACKAGED LINE CHARGES TO SEPARATELY PAYABLE LINES IN PROPORTION TO
    // THEIR PAYMENTS WHEN THERE ARE ARTIFICIAL PROCEDURE CHARGES (< $1.01) ON THE CLAIM.
    // (DISTRIBUTED CHARGES ARE ADDED TO THE BILLED CHARGES)
    if (pricerContext.isNFlag()
        && pricerContext.isSt0Flag()
        && OppsPricerContext.isSeparatelyPayable(statusIndicator)
        && isArtificialOrNotPackaged(line.getPackageFlag())
        && BigDecimalUtils.isGreaterThanZero(pricerContext.getTotalSTVXPayment())) {

      // COMPUTE H-CHRG-RATE ROUNDED = (A-LITEM-PYMT (LN-SUB) / H-TOT-STVX-PYMT)
      // COMPUTE H-SUB-CHRG ROUNDED = (H-CHRG-RATE * H-TOT-N-CHRG)
      // COMPUTE W-SUB-CHRG (W-LP-INDX) ROUNDED = W-SUB-CHRG (W-LP-INDX) + H-SUB-CHRG
      deductibleLine.setSubCharge(
          linePayment
              .divide(pricerContext.getTotalSTVXPayment(), 8, RoundingMode.HALF_UP)
              .multiply(pricerContext.getTotalNCharge())
              .setScale(2, RoundingMode.HALF_UP)
              .add(deductibleLine.getSubCharge()));
    }

    // CALCULATE COMPOSITE APC CHARGES FOR PRIME CODE LINES
    // THE CHARGES OF ALL PACKAGED LINES WITH A COMPOSITE ADJ. FLAG THAT INDICATES THE LINE IS A
    // PART OF A COMPOSITE APC (VALUES 01 - NN...) ARE ACCUMULATED BY COMPOSITE ADJUSTMENT FLAG AND
    // ADDED TO THE CORRESPONDING PRIME (PAYABLE) LINE'S CHARGES.

    // COMPOSITE ADJUSTMENT FLAG > 0 INDICATES COMPOSITE APC
    if (!OppsPricerContext.isZeroValue(line.getCompositeAdjustmentFlag())) {

      // SEARCH COMPOSITE APC TABLE STARTING AT ENTRY #1
      final BigDecimal compositeSubCharge =
          pricerContext.getCompositeSubCharge(line.getCompositeAdjustmentFlag());

      // SERVICE LINE'S PAYMENT ADJUSTMENT FLAG IS IN TABLE, ADD THE PACKAGED SUBMITTED CHARGES TO
      // THE LINE'S CHARGES
      if (compositeSubCharge != null) {

        // COMPUTE W-SUB-CHRG (W-LP-INDX) ROUNDED = W-SUB-CHRG (W-LP-INDX) + W-CMP-TOT-SUB-CHRG
        // (W-CMP-INDX)
        deductibleLine.setSubCharge(deductibleLine.getSubCharge().add(compositeSubCharge));
      }
    }

    calculationContext.getLineCalculation().setPayment(linePayment);
  }

  private boolean isArtificialOrNotPackaged(String packageFlag) {
    return Stream.of(PackageFlag.NOT_PACKAGED_0, PackageFlag.ARTIFICIAL_SURGICAL_3)
        .anyMatch(pf -> pf.is(packageFlag));
  }
}
