package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.api.v2.OppsPaymentData;
import gov.cms.fiss.pricers.opps.core.rules.AdjustProcedureLine;
import gov.cms.fiss.pricers.opps.core.rules.ApplyDailyInpatientCoinsuranceCap;
import gov.cms.fiss.pricers.opps.core.rules.CalculateClaimLine;
import gov.cms.fiss.pricers.opps.core.rules.CalculateDeductibleBloodPercentage;
import gov.cms.fiss.pricers.opps.core.rules.CalculatePassThroughDeviceOffsets;
import gov.cms.fiss.pricers.opps.core.rules.EndOfClaimProcessing;
import gov.cms.fiss.pricers.opps.core.rules.EnforceCmhcOutlierPaymentCaps;
import gov.cms.fiss.pricers.opps.core.rules.InitializeAndValidateClaim;
import gov.cms.fiss.pricers.opps.core.rules.InitializeAndValidateLines;
import gov.cms.fiss.pricers.opps.core.rules.OrderDeductibleTable;
import gov.cms.fiss.pricers.opps.core.rules.adjust_procedure_lines.CalculateLinePayment;
import gov.cms.fiss.pricers.opps.core.rules.adjust_procedure_lines.CalculateOutlierPayment;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.AccumulateClaimTotals;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.AdjustLineCharges;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.CalculateStandardPayment;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.CapReducedCoinsurance;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.CheckErrorCode;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.PopulateCoinsuranceCapTable2024;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.UpdateLineOutput;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.AdjustMaximumNationalReducedCoinsurance;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.AdjustMinimumCoinsurance2024;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.CalculateDeviceCredit;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.CalculateLineReimbursement2024;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.CalculateStatusIndicatorPayments;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.CalculateTerminatedProcedureDeviceOffset;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.SetBloodFraction;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.calculate_status_indicator_payments.BloodProductAndBrachytherapy;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.calculate_status_indicator_payments.PassThroughDeviceCategories;
import gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines.calculate_standard_payment.calculate_status_indicator_payments.ProceduresServicesAndVisits;
import gov.cms.fiss.pricers.opps.core.rules.coinsurance_and_reimbursement_calculation.AdjustBloodCoinsuranceForInpatientLimit;
import gov.cms.fiss.pricers.opps.core.rules.coinsurance_and_reimbursement_calculation.CalculateBloodCoinsuranceToBePaid;
import gov.cms.fiss.pricers.opps.core.rules.coinsurance_and_reimbursement_calculation.ProcessCoinsuranceCaps;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.DetermineCbsa;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.DetermineDeviceCreditAmount;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.DetermineWageIndex;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.SetInitialValues;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.WageIndexCheck;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.WageIndexImputedFloorAdjustment;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.WageIndexLookupAndFloor2023;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.WageIndexOutmigrationAdjustment;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.WageIndexQuartileAdjustment;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim.WageIndexTransitionAdjustment2023;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.ApcLookup;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.CalculateDiscountRate;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.CalculateTotalsStep1;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.CalculateTotalsStep2;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.ColonialProcedureCoinsuranceCheck;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.PerformApcAdjustments2024;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.PopulateDeductibleTable;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.ValidateApcAndPackagingFlag;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.ValidateIoceInputFlags;
import gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines.ValidateServiceUnit;
import gov.cms.fiss.pricers.opps.core.tables.DataTables;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 2024 implementation of the OPPS pricer. */
public class Opps2024RulePricer extends OppsRulePricer {

  public Opps2024RulePricer(DataTables dataTables) {
    super(dataTables, rules());
  }

  /** Returns a list of rules and rule sets to be executed sequentially. */
  private static List<
          CalculationRule<OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext>>
      rules() {
    return List.of(
        new InitializeAndValidateClaim(
            List.of(
                new SetInitialValues(),
                new DetermineDeviceCreditAmount(),
                new DetermineCbsa(),
                new DetermineWageIndex(
                    List.of(
                        new WageIndexLookupAndFloor2023(),
                        new WageIndexImputedFloorAdjustment(),
                        new WageIndexOutmigrationAdjustment(),
                        new WageIndexQuartileAdjustment(),
                        new WageIndexTransitionAdjustment2023())),
                new WageIndexCheck())),
        new InitializeAndValidateLines(
            List.of(
                new ValidateServiceUnit(),
                new ValidateIoceInputFlags(),
                new CalculateDiscountRate(),
                new CalculateTotalsStep1(),
                new ValidateApcAndPackagingFlag(),
                new ApcLookup(),
                new PerformApcAdjustments2024(),
                new CalculateTotalsStep2(),
                new PopulateDeductibleTable())),
        new OrderDeductibleTable(),
        new CalculateDeductibleBloodPercentage(),
        new CalculatePassThroughDeviceOffsets(),
        new CalculateClaimLine(
            List.of(
                new CheckErrorCode(),
                new CalculateStandardPayment(
                    List.of(
                        new SetBloodFraction(),
                        new CalculateDeviceCredit(),
                        new CalculateTerminatedProcedureDeviceOffset(),
                        new CalculateStatusIndicatorPayments(
                            List.of(
                                new BloodProductAndBrachytherapy(),
                                new ProceduresServicesAndVisits(),
                                new PassThroughDeviceCategories())),
                        new CalculateLineReimbursement2024(),
                        new AdjustMinimumCoinsurance2024(),
                        new AdjustMaximumNationalReducedCoinsurance())),
                new PopulateCoinsuranceCapTable2024(),
                new AdjustLineCharges(),
                new AccumulateClaimTotals(),
                new UpdateLineOutput(),
                new CapReducedCoinsurance())),
        new AdjustProcedureLine(List.of(new CalculateLinePayment(), new CalculateOutlierPayment())),
        new EnforceCmhcOutlierPaymentCaps(),
        new ApplyDailyInpatientCoinsuranceCap(
            List.of(
                new ProcessCoinsuranceCaps(
                    List.of(
                        new CalculateBloodCoinsuranceToBePaid(),
                        new AdjustBloodCoinsuranceForInpatientLimit())))),
        new ColonialProcedureCoinsuranceCheck(),
        new EndOfClaimProcessing());
  }

  @Override
  protected OppsPricerContext contextFor(OppsClaimPricingRequest input) {
    final OppsClaimPricingResponse output = new OppsClaimPricingResponse();
    output.setPaymentData(new OppsPaymentData());
    output.getPaymentData().setTotalPayment(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY));

    return new Opps2024PricerContext(input, output, dataTables);
  }
}
