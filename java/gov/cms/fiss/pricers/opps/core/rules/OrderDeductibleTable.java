package gov.cms.fiss.pricers.opps.core.rules;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderDeductibleTable
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  /**
   * Order coinsurance and blood deductible table entries
   *
   * <p>COBOL code orders the CoinsuranceDeductible table by APC Rank. However, certain blood line
   * entries act as place holders and are processed based on the order in the BloodDeductible table
   * (DOS, Blood Rank). This method will order the deductible table accordingly.
   *
   * <p>This replaces the logic in the following COBOL paragraphs: (19300-COIN-DEDUCT)
   * (19350-STAGE-ENTRY) (19375-BLOOD-DEDUCT) (19385-STAGE-ENTRY)
   */
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final List<DeductibleLine> deductibleLines = calculationContext.getDeductibleLines();
    final List<DeductibleLine> reorderedList = new ArrayList<>();
    final List<DeductibleLine> bloodDeductibleLines;

    // Sort table based on APC Rank
    deductibleLines.sort(DeductibleLine::compareByApcRank);

    // Extract blood line entries
    bloodDeductibleLines =
        deductibleLines.stream().filter(l -> l.getBloodRank() != null).collect(Collectors.toList());

    // Sort blood table based on Date of Service followed by Blood Rank
    bloodDeductibleLines.sort(DeductibleLine::compareByDosThenBloodRank);

    // Replace blood entries in deductibleLines with entries in bloodDeductibleLines
    final Iterator<DeductibleLine> iterator = bloodDeductibleLines.iterator();

    for (final DeductibleLine deductibleLine : deductibleLines) {

      final IoceServiceLineData lineInput = deductibleLine.getServiceLine();

      if (calculationContext.isBloodDeductibleHcpcsLine(
          lineInput.getStatusIndicator(),
          lineInput.getHcpcsCode(),
          lineInput.getPaymentAdjustmentFlags())) {

        // Next blood line
        final DeductibleLine bloodDeductibleLine = iterator.next();

        // Locate the coinsurance line that matches the next blood line
        final DeductibleLine replacementLine =
            deductibleLines.stream()
                .filter(
                    e -> Objects.equals(e.getServiceLine(), bloodDeductibleLine.getServiceLine()))
                .findFirst()
                .orElseThrow();

        reorderedList.add(replacementLine);
      } else {
        reorderedList.add(deductibleLine);
      }
    }

    calculationContext.setDeductibleLines(reorderedList);
  }
}
