package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import gov.cms.fiss.pricers.opps.core.codes.PackageFlag;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import java.math.BigDecimal;
import java.util.stream.Stream;

public class CalculateTotalsStep1 extends AbstractLineCalculationRule {
  /**
   * Pre-calculation validations.
   *
   * <p>(Extracted from 19150-INIT)
   */
  @Override
  public void calculate(ServiceLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final IoceServiceLineData ioceServiceLine = calculationContext.getInput();

    // ACCUMULATE TOTAL CHARGES OF ALL LINES THAT PASS VALIDATION RULES

    final BigDecimal subCharge = ioceServiceLine.getCoveredCharges();

    // EXCLUDE LINES THAT RECEIVE EXTERNAL LINE ITEM ADJ.
    if (!OppsPricerContext.hasExternalAdjustment(ioceServiceLine.getActionFlag())) {

      // COMPUTE H-TOT-CHRG = H-TOT-CHRG + H-SUB-CHRG
      pricerContext.setTotalCharge(pricerContext.getTotalCharge().add(subCharge));
    }

    // ACCUMULATE PACKAGED LINE TOTAL CHARGES & SET CLAIM N-FLAG EXCLUDE ALL PACKAGED COMPOSITE
    // LINES
    if (shouldAccumulateNLineCharge(calculationContext)) {
      // COMPUTE H-TOT-N-CHRG = H-SUB-CHRG + H-TOT-N-CHRG
      pricerContext.setTotalNCharge(pricerContext.getTotalNCharge().add(subCharge));

      pricerContext.setNFlag(true);
    }

    // ACCUMULATE PACKAGED REVENUE CODE 39X BLOOD LINE CHARGES
    // (BLOOD PROCESS/STORAGE (LABOR) NOT SBJ TO BLD DEDUC)
    // WHEN BILLED ON CLAIM WITH A COMPREHENSIVE APC TO
    // DETERMINE THE PORTION OF THE BLOOD APC PAYMENT TO BE
    // DISTRIBUTED TO THE BLOOD PRODUCT (PAF = 5) LINE FOR THE
    // BLOOD DEDUCTIBLE CALCULATION
    if (StatusIndicator.N_PACKAGED_INTO_APC.is(ioceServiceLine.getStatusIndicator())) {
      if (pricerContext.getComprehensiveApcClaimStatus()
          && OppsPricerContext.LINE_REVENUE_CODE.contains(ioceServiceLine.getRevenueCode())) {

        // COMPUTE H-TOT-38X-39X = OPPS-SUB-CHRG (LN-SUB) + H-TOT-38X-39X
        pricerContext.setTotalBloodCharges(pricerContext.getTotalBloodCharges().add(subCharge));
      }

      // ACCUMULATE NON-PRIME (PACKAGED) COMPOSITE APC CHARGES
      // FOR EACH COMPOSITE APC BY THE COMPOSITE ADJUSTMENT FLAG
      // (POPULATE COMPOSITE TABLE)
      if (!OppsPricerContext.isZeroValue(ioceServiceLine.getCompositeAdjustmentFlag())) {
        calculateCompositeSubCharges(calculationContext);
      }
    }
  }

  /** (Extracted from 19150-INIT). */
  protected boolean shouldAccumulateNLineCharge(ServiceLineContext calculationContext) {
    final IoceServiceLineData line = calculationContext.getInput();
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final boolean comprehensiveApcClaimFlag = pricerContext.getComprehensiveApcClaimStatus();

    // IF ( (OPPS-PKG-FLAG (LN-SUB) = '1' OR '2' OR '4') AND
    //   (OPPS-COMP-ADJ-FLAG (LN-SUB) = '00') ) OR
    //    PKG-BLD-DED-LINE

    return isPackaged(line.getPackageFlag())
            && OppsPricerContext.isZeroValue(line.getCompositeAdjustmentFlag())
        || OppsPricerContext.isComprehensiveBloodDeductible(
            line.getStatusIndicator(), line.getPaymentAdjustmentFlags(), comprehensiveApcClaimFlag);
  }

  /**
   * ACCUMULATE NON-PRIME COMPOSITE APC CHARGES FOR EACH COMPOSITE APC USING PACKAGED LINES WITH
   * COMPOSITE ADJUSTMENT FLAG NOT = '00' &amp; POPULATE COMPOSITE APC TABLE.
   *
   * <p>(19170-COMPOSITES)
   *
   * <p>(19171-SEARCH-CAF)
   *
   * <p>(19172-ADD-ENTRY)
   *
   * <p>(19173-UPDATE-ENTRY)
   */
  protected void calculateCompositeSubCharges(ServiceLineContext calculationContext) {
    final IoceServiceLineData ioceServiceLine = calculationContext.getInput();
    calculationContext
        .getPricerContext()
        .addCompositeSubCharge(
            ioceServiceLine.getCompositeAdjustmentFlag(), ioceServiceLine.getCoveredCharges());
  }

  private boolean isPackaged(String packageFlag) {
    return Stream.of(
            PackageFlag.SERVICE_1, PackageFlag.PER_DIEM_2, PackageFlag.DRUG_ADMINISTRATION_4)
        .anyMatch(pf -> pf.is(packageFlag));
  }
}
