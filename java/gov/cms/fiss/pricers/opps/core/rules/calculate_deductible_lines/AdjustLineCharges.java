package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.PackageFlag;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.math.BigDecimal;
import java.util.stream.Stream;

public class AdjustLineCharges
    implements CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  /**
   * IDENTIFY CLAIMS THAT HAVE SIGNIFICANT PROCEDURE (SURGERY) LINE(S) WITH ARTIFICIAL CHARGES FOR
   * FURTHER PROCESSING. ACCUMULATE TOTAL CLAIM PAYMENTS AND CHARGES FOR SIGNIFICANT PROCEDURE
   * LINES, AND ACCUMULATE TOTAL CLAIM CHARGES OF ALL SEPARATELY PAYABLE LINES.
   *
   * <p>FLAG CLAIMS WITH ARTIFICIAL CHARGES AND TOTAL PAYMENTS &amp; CHARGES FOR ARTIFICIAL CHARGE
   * AND PACKAGING PROCESSING.
   *
   * <p>(19500-ADJ-CHRGS)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final IoceServiceLineData lineInput = calculationContext.getInput().getServiceLine();
    final LineCalculation lineCalculation = calculationContext.getLineCalculation();
    final DeductibleLine deductibleLine = lineCalculation.getDeductibleLine();
    final boolean comprehensiveApcClaimFlag = pricerContext.getComprehensiveApcClaimStatus();
    final String statusIndicator = lineInput.getStatusIndicator();
    final String packageFlag = lineInput.getPackageFlag();

    // PKG-BLD-DED-LINE-FLAG
    final boolean packagedBloodDeductibleLineFlag =
        OppsPricerContext.isComprehensiveBloodDeductible(
                statusIndicator, lineInput.getPaymentAdjustmentFlags(), comprehensiveApcClaimFlag)
            && pricerContext.getDataTables().isBloodHcpcsDeductible(lineInput.getHcpcsCode());

    // FLAG CLAIM WHEN A SIGNIFICANT PROCEDURE LINE HAS ARTIFICIAL CHARGES ($1 OR LESS) - FLAGS
    // ENTIRE CLAIM
    if (OppsPricerContext.isSignificantProcedure(statusIndicator, lineInput.getHcpcsCode())) {
      if (BigDecimalUtils.isLessThan(deductibleLine.getSubCharge(), new BigDecimal("1.01"))) {
        pricerContext.setSt0Flag(true);
      }

      // ACCUMULATE TOTAL CLAIM CHARGES AND PAYMENTS FROM UNPACKAGED SIGNIFICANT PROCEDURE (SURGERY)
      // LINES
      if (isArtificialOrNotPackaged(packageFlag)) {
        // COMPUTE H-TOT-ST-CHRG = W-SUB-CHRG (W-LP-INDX) + H-TOT-ST-CHRG
        pricerContext.setTotalSTCharge(
            pricerContext.getTotalSTCharge().add(deductibleLine.getSubCharge()));

        // COMPUTE H-TOT-ST-PYMT = H-LITEM-PYMT + H-TOT-ST-PYMT
        pricerContext.setTotalSTPayment(
            pricerContext.getTotalSTPayment().add(lineCalculation.getPayment()));
      }
    }

    // ACCUMULATE TOTAL CLAIM PAYMENTS FROM UNPACKAGED SEPARATELY PAYABLE LINES (FOR PACKAGING
    // LATER)
    if (OppsPricerContext.isSeparatelyPayable(statusIndicator)
        && isArtificialOrNotPackaged(packageFlag)
        && !packagedBloodDeductibleLineFlag) {

      // COMPUTE H-TOT-STVX-PYMT = H-LITEM-PYMT + H-TOT-STVX-PYMT
      pricerContext.setTotalSTVXPayment(
          pricerContext.getTotalSTVXPayment().add(lineCalculation.getPayment()));
    }
  }

  private boolean isArtificialOrNotPackaged(String packageFlag) {
    return Stream.of(PackageFlag.NOT_PACKAGED_0, PackageFlag.ARTIFICIAL_SURGICAL_3)
        .anyMatch(pf -> pf.is(packageFlag));
  }
}
