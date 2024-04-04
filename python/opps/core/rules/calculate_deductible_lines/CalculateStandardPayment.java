package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.application.rules.EvaluatingCalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.codes.PaymentIndicator;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import java.util.List;
import java.util.stream.Stream;

public class CalculateStandardPayment
    extends EvaluatingCalculationRule<
        DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  public CalculateStandardPayment(
      List<CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext>>
          calculationRules) {
    super(calculationRules);
  }

  private static boolean hasPayableStatus(String paymentIndicator) {
    return Stream.of(
            PaymentIndicator.PAID_STANDARD_HOSPITAL_OPPS_AMOUNT_1,
            PaymentIndicator.PAID_STANDARD_AMOUNT_FOR_PASS_THROUGH_DRUG_OR_BIOLOGICAL_5,
            PaymentIndicator.PAYMENT_BASED_ON_CHARGE_ADJUSTED_TO_COST_6,
            PaymentIndicator.ADDITIONAL_PAYMENT_FOR_NEW_DRUG_OR_BIOLOGICAL_7,
            PaymentIndicator.PAID_PARTIAL_HOSPITALIZATION_PER_DIEM_8)
        .anyMatch(pi -> pi.is(paymentIndicator));
  }

  /**
   * CALCULATE THE FOLLOWING FOR VALID LINES IN THE COINSURANCE DEDUCTIBLE TABLE WITH A PAYABLE
   * STATUS:
   *
   * <pre>
   *   - LINE ITEM PAYMENT (BASED ON LINE SERVICE INDICATOR)
   *   - BENEFICIARY DEDUCTIBLE (IN ORDER OF APC RANK)
   *   - MINIMUM, MAXIMUM, AND REDUCED COINSURANCE AMOUNTS
   *
   * 1. CALCULATE STANDARD LINE ITEM PAYMENT (LINE PRICE)
   *    (APC PAYMENT * WAGE INDEX * SERVICE UNITS *
   *     DISCOUNT FACTOR)
   *    WAGE ADJUST 60% OF THE APC PAYMENT FOR SI = S, V, T, P,
   *    OR X LINES, THESE ARE ELIGIBLE FOR THE SCH ADJUSTMENT
   * 2. APPLY DEDUCTIBLE TO HIGHEST NATIONAL COINSURANCE AMOUNT
   *    DESCENDING UNTIL DEDUCTIBLE = 0.
   * 3. ADD LINE PRICE TO OUTLIER HOLD AREA
   * 4. CALCULATE THE LINE PRICE FOR DESIGNATED DEVICES
   *    &amp; TAKE PT DEVICE OFFSET WHEN APPLICABLE
   * 5. CALCULATE AND APPLY DEVICE CREDIT
   * </pre>
   *
   * <p>(19550-CALC-STANDARD)
   */
  @Override
  public void calculate(DeductibleLineContext calculationContext) {
    // CALCULATE THE FOLLOWING FOR VALID LINES IN THE COINSURANCE
    // DEDUCTIBLE TABLE WITH A PAYABLE STATUS:
    //   - LINE ITEM PAYMENT (BASED ON LINE SERVICE INDICATOR)
    //   - BENEFICIARY DEDUCTIBLE (IN ORDER OF APC RANK)
    //   - MINIMUM, MAXIMUM, AND REDUCED COINSURANCE AMOUNTS
    //   - LINE ITEM REIMBURSEMENT IF MAX COIN LIMIT EXCEEDED
    final String paymentIndicator =
        calculationContext.getInput().getServiceLine().getPaymentIndicator();
    if (hasPayableStatus(paymentIndicator)) {
      super.calculate(calculationContext);
    } else {
      calculationContext.setCalculationCompleted();
    }
  }
}
