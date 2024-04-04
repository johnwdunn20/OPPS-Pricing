package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.calculate_status_indicator_payments;

import gov.cms.fiss.pricers.common.api.OutpatientProviderData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.util.stream.Stream;

public class ProceduresServicesAndVisits extends AbstractStatusIndicatorPayments2024 {

  /** CALCULATE PAYMENT FOR SI = S, V, T, P, X, J1, OR J2 LINES. */
  @Override
  public boolean shouldExecute(DeductibleLineContext context) {
    return isProcedureServiceOrVisit(context.getStatusIndicator());
  }

  /**
   * Calculates payment based on status indicators.
   *
   * <p>(Extracted from 19550-CALC-STANDARD)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    final LineCalculation lineCalculation = calculationContext.getLineCalculation();
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final OutpatientProviderData providerData = pricerContext.getProviderData();
    final String billType = pricerContext.getClaimData().getTypeOfBill();

    calculateWageAdjustedPaymentAndSchAdj(pricerContext, providerData, lineCalculation, billType);
    pricerContext.setBeneficiaryDeductible(
        calculateBeneficiaryDeductible(lineCalculation, pricerContext.getBeneficiaryDeductible()));
  }

  /** Returns true if status indicator is a procedure, service or visit. */
  private boolean isProcedureServiceOrVisit(String statusIndicator) {
    return Stream.of(
            StatusIndicator.P_PARTIAL_HOSPITALIZATION,
            StatusIndicator.S_PROCEDURE_NOT_DISCOUNTED,
            StatusIndicator.T_PROCEDURE_REDUCIBLE,
            StatusIndicator.V_EMERGENCY,
            StatusIndicator.X_ANCILLARY,
            StatusIndicator.J1_COMPREHENSIVE_APC_OUTPATIENT,
            StatusIndicator.J2_COMPREHENSIVE_APC_HOSPITAL)
        .anyMatch(si -> si.is(statusIndicator));
  }
}
