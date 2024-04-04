package gov.cms.fiss.pricers.opps.core.rules.calculate_deductible_lines;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.DeductibleLineContext;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import gov.cms.fiss.pricers.opps.core.model.CoinsuranceCapEntry;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.stream.Stream;

// POPULATE DRUG COINSURANCE TABLE FOR LATER PROCESSING
public class PopulateCoinsuranceCapTable
    implements CalculationRule<DeductibleLine, ServiceLinePaymentData, DeductibleLineContext> {

  /** Rule only applies to lines without an error code. */
  @Override
  public boolean shouldExecute(DeductibleLineContext calculationContext) {
    return Integer.parseInt(calculationContext.getOutput().getReturnCode().getCode()) < 30;
  }

  /**
   * POPULATE COINSURANCE CAP ROLL-UP TABLE WITH THE COINSURANCE AMOUNTS OF SERVICE LINES IN THE
   * COINSURANCE DEDUCTIBLE TABLE.
   *
   * <pre>
   * COINSURANCE RECORD COMBINATIONS:
   *   - TYPE 1, COIN1 = 0, COIN2 &gt; 0, &amp; SI = X =&gt;
   *       BLOOD ADMINISTERED INDEPENDENT OF A PROCEDURE VISIT
   *       ON THE DATE OF SERVICE
   *   - TYPE 1, COIN1 &gt; 0, COIN2 = 0, &amp; SI = S, V, OR T =&gt;
   *       PROCEDURE / VISIT WITH NO BLOOD ADMINISTERED ON THE
   *       DATE OF SERVICE
   *   - TYPE 1, COIN1 &gt; 0, COIN2 &gt; 0, &amp; SI = S, V, OR T =&gt;
   *       PROCEDURE / VISIT WITH BLOOD ADMINISTERED ON THE
   *       DATE OF SERVICE
   *   - TYPE 2, COIN1 = 0, COIN2 &gt; 0, &amp; SI = R =&gt;
   *       BLOOD ADMINSTERED ON THE DATE OF SERVICE
   * </pre>
   *
   * <p>(19450-ADJ-PROC-COIN)
   */
  @Override
  public void calculate(DeductibleLineContext deductibleLineContext) {
    final OppsPricerContext calculationContext = deductibleLineContext.getPricerContext();
    final LineCalculation lineCalculation = deductibleLineContext.getLineCalculation();

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    final DeductibleLine deductibleLine = lineCalculation.getDeductibleLine();

    final String statusIndicator = lineInput.getStatusIndicator();
    final BigDecimal coinsuranceAmount; // H-NEW-COIN

    // Extracted from 19550-CALC-STANDARD
    // PKG-BLD-DED-LINE-FLAG
    final boolean packagedBloodDeductibleLineFlag =
        OppsPricerContext.isComprehensiveBloodDeductible(
                lineInput.getStatusIndicator(),
                lineInput.getPaymentAdjustmentFlags(),
                calculationContext.getComprehensiveApcClaimStatus())
            && calculationContext.getDataTables().isBloodHcpcsDeductible(lineInput.getHcpcsCode());

    // Determine if rule should exit based on blood package and/or section 603 status
    if (packagedBloodDeductibleLineFlag
        || OppsPricerContext.isSection603(lineInput.getPaymentMethodFlag())) {
      return;
    }

    // PROCESS SI = S, T, OR V LINES (PROCEDURE OR VISIT)
    if (isSignificantProcedureOrClinicVisit(statusIndicator)) {

      // CALCULATE WAGE ADJUSTED LINE NATIONAL COINSURANCE AMOUNT
      // COMPUTE H-NEW-WGNAT ROUNDED = W-NAT-COIN (W-LP-INDX) * (.6 * W-WINX (W-LP-INDX) + .4)
      BigDecimal nationalWageAdjustedCoinsurance =
          deductibleLine
              .getNationalCoinsurance()
              .multiply(
                  deductibleLine
                      .getWageIndex()
                      .multiply(new BigDecimal(".60"))
                      .add(new BigDecimal(".40")))
              .setScale(2, RoundingMode.HALF_UP);

      // ENFORCE INPATIENT LIMIT ON NATIONAL COINSURANCE AMOUNT
      if (BigDecimalUtils.isGreaterThan(
          nationalWageAdjustedCoinsurance, calculationContext.getInpatientDeductibleLimit())) {
        nationalWageAdjustedCoinsurance = calculationContext.getInpatientDeductibleLimit();
      }

      // CALCULATE LINE NEW COINSURANCE AMOUNT (ACTUAL COIN DUE)
      // COMPUTE H-NEW-COIN = H-LITEM-PYMT - H-TOTAL-LN-DEDUCT - H-LITEM-REIM - H-LN-BLOOD-DEDUCT
      coinsuranceAmount =
          lineCalculation
              .getPayment()
              .subtract(lineCalculation.getTotalDeductible())
              .subtract(lineCalculation.getReimbursement())
              .subtract(lineCalculation.getBloodDeductible());

      // INDICATE TYPE 1 RECORD (ONE TYPE 1 RECORD PER DAY)
      addOrUpdateCoinsuranceCapTable(
          calculationContext, lineCalculation, nationalWageAdjustedCoinsurance, coinsuranceAmount);

    }
    // PROCESS SI = R LINES (BLOOD)
    else if (StatusIndicator.R_BLOOD.is(statusIndicator)) {
      final BigDecimal nationalWageAdjustedCoinsurance = BigDecimalUtils.ZERO; // H-NEW-WGNAT

      // CALCULATE LINE NEW COINSURANCE AMOUNT (ACTUAL COIN DUE)
      // COMPUTE H-NEW-COIN = H-LITEM-PYMT - H-TOTAL-LN-DEDUCT - H-LITEM-REIM - H-LN-BLOOD-DEDUCT
      coinsuranceAmount =
          lineCalculation
              .getPayment()
              .subtract(lineCalculation.getTotalDeductible())
              .subtract(lineCalculation.getReimbursement())
              .subtract(lineCalculation.getBloodDeductible());

      // SET BLOOD-FLAG TO INDICATE BLOOD LINE
      calculationContext.setBloodFlag(true);
      addOrUpdateCoinsuranceCapTable(
          calculationContext, lineCalculation, nationalWageAdjustedCoinsurance, coinsuranceAmount);

      // INDICATE TYPE 2 RECORD (ONE TYPE 2 REC FOR EVERY BLOOD LINE)
      addCoinsuranceCapEntry(
          calculationContext,
          2,
          lineCalculation,
          nationalWageAdjustedCoinsurance,
          coinsuranceAmount);
    }
  }

  /**
   * DETERMINE WHETHER A NEW COINSURANCE CAP ROLL-UP TABLE RECORD SHOULD BE ADDED OR IF AN EXISTING
   * RECORD NEEDS TO BE UPDATED (RECORD KEY IS DATE OF SERVICE &amp; RECORD TYPE).
   *
   * <p>(19455-SEARCH-KEY)
   */
  protected void addOrUpdateCoinsuranceCapTable(
      OppsPricerContext calculationContext,
      LineCalculation lineCalculation,
      BigDecimal nationalWageAdjustedCoinsurance,
      BigDecimal coinsuranceAmount) {

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    final LocalDate dos = lineCalculation.getLineInput().getDateOfService();
    final CoinsuranceCapEntry entry =
        calculationContext.getCoinsuranceCaps().stream()
            .filter(e -> e.getDateOfService().isEqual(dos) && e.getCode().equals(1))
            .findAny()
            .orElse(null);

    // SEARCH COINSURANCE CAP TABLE
    // IF THE SERVICE LINE'S DATE OF SERVICE AND RECORD TYPE COMBO IS NOT ALREADY IN THE TABLE, ADD
    // IT
    if (entry == null) {
      addCoinsuranceCapEntry(
          calculationContext,
          1,
          lineCalculation,
          nationalWageAdjustedCoinsurance,
          coinsuranceAmount);
    }
    // IF THE SERVICE LINE'S DATE OF SERVICE AND RECORD TYPE COMBO IS ALREADY IN THE TABLE, UPDATE
    // THE ENTRY
    else {
      updateCoinsuranceCapEntry(
          entry, lineInput, nationalWageAdjustedCoinsurance, coinsuranceAmount);
    }
  }

  /**
   * UPDATE THE EXISTING COINSURANCE RECORD WITH THE SAME DATE OF SERVICE AND RECORD TYPE OF THE
   * CURRENT SERVICE LINE.
   *
   * <p>(19465-UPDATE-ENTRY, 19485-REPLACE-TYPE1, 19480-RANK-COIN)
   */
  protected void updateCoinsuranceCapEntry(
      CoinsuranceCapEntry entry,
      IoceServiceLineData lineInput,
      BigDecimal nationalWageAdjustedCoinsurance,
      BigDecimal coinsuranceAmount) {

    final String statusIndicator = lineInput.getStatusIndicator();
    // FOR BLOOD LINES - ACCUMULATE DAY'S TOTAL BLOOD COIN DUE
    if (StatusIndicator.R_BLOOD.is(statusIndicator)) {
      entry.setCoinsurance2(entry.getCoinsurance2().add(coinsuranceAmount));
    }
    // UPDATE THE RECORD FOR LINES INITIALLY CREATED FOR A BLOOD LINE
    else if (StatusIndicator.X_ANCILLARY.is(entry.getStatusIndicator())) {
      entry.setServiceLine(lineInput);
      entry.setCoinsurance1(coinsuranceAmount);
      entry.setNationalWageAdjustedCoinsurance(nationalWageAdjustedCoinsurance);
      entry.setStatusIndicator(statusIndicator);
    } else {
      // FOR MULTIPLE PROCEDURES/VISITS ON THE SAME DATE OF SERVICE USE HIGHEST COIN AMOUNT
      if (BigDecimalUtils.isGreaterThan(coinsuranceAmount, entry.getCoinsurance1())) {
        entry.setServiceLine(lineInput);
        entry.setCoinsurance1(coinsuranceAmount);
        entry.setNationalWageAdjustedCoinsurance(nationalWageAdjustedCoinsurance);
        entry.setStatusIndicator(statusIndicator);
      }
    }
  }

  /**
   * ADD COIN CAP RECORD BASED ON SERVICE INDICATOR.
   *
   * <p>(19460-ADD-ENTRY)
   */
  protected void addCoinsuranceCapEntry(
      OppsPricerContext calculationContext,
      int code,
      LineCalculation lineCalculation,
      BigDecimal nationalWageAdjustedCoinsurance,
      BigDecimal coinsuranceAmount) {

    final IoceServiceLineData lineInput = lineCalculation.getLineInput();
    final String statusIndicator = lineInput.getStatusIndicator();

    final CoinsuranceCapEntry entry = new CoinsuranceCapEntry();
    entry.setServiceLine(lineInput);
    entry.setDateOfService(lineInput.getDateOfService());
    entry.setCode(code);

    // ONE TYPE 2 REC FOR EVERY BLOOD LINE
    if (code == 2) {
      entry.setCoinsurance1(BigDecimalUtils.ZERO);
      entry.setNationalWageAdjustedCoinsurance(nationalWageAdjustedCoinsurance);
      entry.setCoinsurance2(coinsuranceAmount);
      entry.setStatusIndicator(statusIndicator);
    }
    // POPULATE THE EMPTY RECORD WITH THE NEW SERVICE LINE'S DATA RECORD TYPE 1, BLOOD
    else if (StatusIndicator.R_BLOOD.is(statusIndicator)) {
      entry.setCoinsurance1(BigDecimalUtils.ZERO);
      entry.setNationalWageAdjustedCoinsurance(BigDecimalUtils.ZERO);
      entry.setCoinsurance2(coinsuranceAmount);
      entry.setStatusIndicator(StatusIndicator.X_ANCILLARY.getIndicator());
    }
    // POPULATE THE EMPTY RECORD WITH THE NEW SERVICE LINE'S DATA RECORD TYPE 1, PROCEDURE OR VISIT
    else {
      entry.setCoinsurance1(coinsuranceAmount);
      entry.setNationalWageAdjustedCoinsurance(nationalWageAdjustedCoinsurance);
      entry.setCoinsurance2(BigDecimalUtils.ZERO);
      entry.setStatusIndicator(statusIndicator);
    }

    calculationContext.addCoinsuranceCap(entry);
  }

  private boolean isSignificantProcedureOrClinicVisit(String statusIndicator) {
    return Stream.of(
            StatusIndicator.S_PROCEDURE_NOT_DISCOUNTED,
            StatusIndicator.T_PROCEDURE_REDUCIBLE,
            StatusIndicator.V_EMERGENCY)
        .anyMatch(si -> si.is(statusIndicator));
  }
}
