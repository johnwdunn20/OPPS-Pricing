package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.common.api.AmbulatoryPaymentClassificationData;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import gov.cms.fiss.pricers.opps.core.codes.StatusIndicator;
import gov.cms.fiss.pricers.opps.core.model.APCCalculationData;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PopulateDeductibleTable extends AbstractLineCalculationRule {

  @Override
  public boolean shouldExecute(ServiceLineContext calculationContext) {
    return List.of(
            ReturnCode.PROCESSED_1.getCode(), ReturnCode.ABSENT_QUALITY_REPORTING_11.getCode())
        .contains(Integer.parseInt(calculationContext.getOutput().getReturnCode().getCode()));
  }

  /**
   * Populates the deductible table with coinsurance and blood deductible entries.
   *
   * <p>(Extracted from 19150-INIT and 19300-COIN-DEDUCT)
   */
  @Override
  public void calculate(ServiceLineContext calculationContext) {
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final IoceServiceLineData ioceServiceLine = calculationContext.getInput();
    final APCCalculationData apcData = pricerContext.getApcCalculationData();

    final DeductibleLine deductibleLine =
        new DeductibleLine(
            ioceServiceLine,
            apcData,
            pricerContext.getApcPayment(),
            pricerContext.getWageIndex(),
            pricerContext.getDiscountRate());

    // PARTIAL HOSPITALIZATION LINES RECEIVE 1 SERVICE UNIT
    if (StatusIndicator.P_PARTIAL_HOSPITALIZATION.is(ioceServiceLine.getStatusIndicator())) {
      deductibleLine.setServiceUnits(1);
    }

    // Sorting is deferred until after all lines have been added. This diverges from the logic in
    // paragraphs 19300-COIN-DEDUCT & 19350-STAGE-ENTRY
    pricerContext.getDeductibleLines().add(deductibleLine);

    // SEARCH PROVIDER SPECIFIC FILE FOR THE LINE APC.
    // IF FOUND, CALCULATE THE REDUCED COINSURANCE AMOUNT FOR
    // THE LINE, ENTER IT INTO THE NEW COIN DEDUC TABLE REC, &
    // RETURN CODE 25
    final BigDecimal reducedCoinsurance =
        getReducedCoinsurance(
            pricerContext.getProviderData().getReducedCoinsuranceData(),
            ioceServiceLine,
            deductibleLine.getDiscountRate());

    if (reducedCoinsurance != null) {
      deductibleLine.setReducedCoinsurance(reducedCoinsurance);
      calculationContext.applyLineReturnCode(ReturnCode.COINSURANCE_REDUCTION_25);
      return;
    }

    final Integer bloodRank =
        pricerContext.getDataTables().getBloodRank(ioceServiceLine.getHcpcsCode());
    // Instead of maintaining two lists we are just updating the blood rank and date of service
    if (bloodRank != null
        && OppsPricerContext.isBloodPaymentAdjustment(
            ioceServiceLine.getPaymentAdjustmentFlags())) {
      deductibleLine.setBloodRank(bloodRank);
      deductibleLine.setDateOfService(ioceServiceLine.getDateOfService());
    }
  }

  /**
   * Calculates and returns the Reduced Coinsurance amount if the Payment APC is listed in the PSF
   * APC table.
   */
  protected BigDecimal getReducedCoinsurance(
      List<AmbulatoryPaymentClassificationData> apcTable,
      IoceServiceLineData ioceServiceLine,
      BigDecimal discountRate) {

    if (apcTable != null) {
      for (final AmbulatoryPaymentClassificationData apcData : apcTable) {
        // 10  OPPS-GRP.
        //   15  FILLER               PIC X(01).
        //   15  OPPS-APC             PIC X(04).
        final String oppsApc = ioceServiceLine.getPaymentApc().substring(1);
        if (apcData.getAmbulatoryPaymentClassificationCode().equals(oppsApc)) {

          // COMPUTE W-RED-COIN (W-LP-INDX) ROUNDED = L-PSF-RED-COIN (PS-SUB) * H-SRVC-UNITS *
          // H-DISC-RATE
          return apcData
              .getReducedCoinsuranceAmount()
              .multiply(new BigDecimal(ioceServiceLine.getApcServiceUnits()))
              .multiply(discountRate)
              .setScale(2, RoundingMode.HALF_UP);
        }
      }
    }

    return null;
  }
}
