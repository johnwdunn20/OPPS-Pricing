package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import gov.cms.fiss.pricers.opps.core.model.APCCalculationData;
import gov.cms.fiss.pricers.opps.core.tables.ApcRateHistoryEntry;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;

public class ApcLookup extends AbstractLineCalculationRule {

  /** (Extracted from 19150-INIT, 19175-APC-LOOKUP). */
  @Override
  public void calculate(ServiceLineContext calculationContext) {
    final APCCalculationData apcData = new APCCalculationData();
    final OppsPricerContext pricerContext = calculationContext.getPricerContext();
    final IoceServiceLineData ioceServiceLine = calculationContext.getInput();
    final LocalDate serviceDate = pricerContext.getClaimData().getServiceFromDate();
    final ApcRateHistoryEntry rateHistoryEntry;

    calculationContext.getPricerContext().setApcCalculationData(apcData);

    // Get APC rate history entries
    final NavigableMap<LocalDate, ApcRateHistoryEntry> entries =
        pricerContext.getDataTables().getApcRateHistoryEntry(ioceServiceLine.getPaymentApc());

    // If payment APC not found in rate table set return code to 30
    if (entries == null) {
      calculationContext.applyLineReturnCode(ReturnCode.APC_NOT_FOUND_30);
      return;
    }

    // GET APC DATA FROM THE REC WITH THE CORRECT EFFECTIVE DATE
    final Map.Entry<LocalDate, ApcRateHistoryEntry> mapEntry = entries.floorEntry(serviceDate);
    if (mapEntry == null) {
      rateHistoryEntry = null;
    } else {
      rateHistoryEntry = mapEntry.getValue().copyBuilder().build();
    }

    // Entry can be null if a record does not exist for both APC and service date
    if (rateHistoryEntry != null) {
      // APC RECORD EFFECTIVE DATE CORRECT, APC ACTIVE - ACCEPT REC
      pricerContext.setApcPayment(rateHistoryEntry.getPaymentRate());
      apcData.setRank(rateHistoryEntry.getRank());
      apcData.setMinimumCoinsurance(rateHistoryEntry.getMinimumCoinsurance());
      apcData.setNationalCoinsurance(rateHistoryEntry.getNationalCoinsurance());
      apcData.setRate(rateHistoryEntry.getReimbursementPercent());
    }
  }
}
