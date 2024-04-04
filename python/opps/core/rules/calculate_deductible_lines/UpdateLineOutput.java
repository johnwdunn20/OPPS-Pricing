package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;

/** Applies calculated line values to the output line record. */
public class UpdateLineOutput
    implements CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final ServiceLinePaymentData lineOutput = calculationContext.getOutput();
    final LineCalculation lineCalculation = calculationContext.getLineCalculation();

    // MOVE LINE VALUES TO VARIABLES TO BE PASSED BACK
    // THE LINE REIM. & ADJ-COIN IS ADJUSTED LATER (IF NEEDED)
    // FOR THE INPATIENT DAILY LIMIT IN 15840-PROCESS-TYPE2
    lineOutput.setPayment(lineCalculation.getPayment());
    lineOutput.setReimbursementAmount(lineCalculation.getReimbursement());
    lineOutput.setTotalDeductible(lineCalculation.getTotalDeductible());
    lineOutput.setBloodDeductible(lineCalculation.getBloodDeductible());
    lineOutput.setCoinsuranceAmount(lineCalculation.getNationalCoinsurance());
    lineOutput.setReducedCoinsurance(lineCalculation.getReducedCoinsurance());
  }
}
