package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.api.OutpatientProviderData;
import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import gov.cms.fiss.pricers.opps.core.tables.CbsaWageIndexEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;

/** APPLY WAGE INDEX FLOOR POLICY (USE HIGHER OF CBSA WAGE INDEX AND STATE RURAL WAGE INDEX). */
public class WageIndexLookupAndFloor
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  /**
   * LOCATE THE CBSA RECORD WITH THE APPROPRIATE EFFECTIVE DATE. WHEN FOUND, SELECT THE APPLICABLE
   * WAGE INDEX VALUE
   *
   * <p>(19210-WAGE-LOOKUP)
   */
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final OutpatientProviderData providerData = calculationContext.getProviderData();
    final String specialPaymentIndicator = providerData.getSpecialPaymentIndicator();

    // CALL WAGE INDEX LOOKUP AND LOOK FOR THE STATE FLOOR.
    final BigDecimal stateFloorWageIndex = getWageIndexByState(calculationContext);
    // GET THE CBSA WAGE INDEX VALUE (IF NOT ALREADY OVERRIDDEN BY THE PSF SPECIAL WAGE INDEX VALUE)
    final CbsaWageIndexEntry cbsaWageIndexEntry = getWageIndexEntryByCbsa(calculationContext);

    // PSF CBSA NOT FOUND IN CBSA TABLE, RETURN ERROR
    if (cbsaWageIndexEntry == null) {
      return;
    }

    // THIS PROVIDER HAS RECLASSIFIED, USE THE WAGE INDEX IN THE SECOND COLUMN FOR RECLASSIFYING
    // PROVIDERS.
    // IF WAGE INDEX > FLOOR, OVERWRITE H-WINX
    if ("Y".equals(specialPaymentIndicator)) {
      // USE HIGHER OF CBSA WAGE INDEX AND STATE RURAL WAGE INDEX
      calculationContext.setWageIndex(
          stateFloorWageIndex.max(cbsaWageIndexEntry.getReclassifiedWageIndex()));
    }
    // THIS PROVIDER HAS NOT RECLASSIFIED, USE THE WAGE INDEX IN THE FIRST COLUMN FOR AREA
    // PROVIDERS.
    // IF WAGE INDEX > FLOOR  -STORE  RESULT IN H-WINX.
    else {
      // USE HIGHER OF CBSA WAGE INDEX AND STATE RURAL WAGE INDEX
      calculationContext.setWageIndex(
          stateFloorWageIndex.max(cbsaWageIndexEntry.getGeographicWageIndex()));
    }
  }

  /**
   * Locate the CBSA record with the appropriate effective date. When found, select the rural floor
   * wage index.
   *
   * <p>(Extracted from 19200-CALC-WAGEINDX)
   */
  private BigDecimal getWageIndexByState(OppsPricerContext context) {
    final String stateCode = context.getProviderData().getStateCode();
    final LocalDate serviceDate = context.getClaimData().getServiceFromDate();

    // Search the CBSA table for the state code
    final NavigableMap<LocalDate, CbsaWageIndexEntry> wageIndices =
        context.getDataTables().getCbsaWageIndexEntries(stateCode);

    // State code not found in CBSA table, return 0
    if (wageIndices == null) {
      return BigDecimal.ZERO;
    }

    // GET WAGE INDEX FROM THE REC W/ THE CORRECT EFFECTIVE DATE
    final CbsaWageIndexEntry entry = lookupWageIndex(wageIndices, serviceDate);
    // If not found, return 0 else the rural floor wage index
    return entry == null ? BigDecimalUtils.ZERO : entry.getRuralFloorWageIndex();
  }

  /**
   * SEARCH WAGE INDEX TABLE FOR THE CBSA IN THE PROVIDER SPECIFIC FILE (PSF).
   *
   * <pre>
   * IF CBSA NOT LOCATED
   *    - SET CLAIM RETURN CODE TO '50'
   *    - DISCONTINUE CLAIM PROCESSING
   * IF WAGE INDEX EQUALS ZERO
   *    - SET CLAIM RETURN CODE TO '51'
   *    - DISCONTINUE CLAIM PROCESSING
   * </pre>
   *
   * <p>(19200-CALC-WAGEINDX)
   */
  private CbsaWageIndexEntry getWageIndexEntryByCbsa(OppsPricerContext context) {
    final String cbsa = context.getPaymentData().getFinalCbsa();
    final LocalDate serviceDate = context.getInput().getClaimData().getServiceFromDate();

    // SEARCH CBSA TABLE FOR THE PSF CBSA
    final NavigableMap<LocalDate, CbsaWageIndexEntry> wageIndices =
        context.getDataTables().getCbsaWageIndexEntries(cbsa);

    // PSF CBSA NOT FOUND IN CBSA TABLE, RETURN ERROR
    if (wageIndices == null) {
      context.applyClaimReturnCode(ReturnCode.WAGE_INDEX_NOT_FOUND_50);
      return null;
    }

    // GET WAGE INDEX FROM THE REC W/ THE CORRECT EFFECTIVE DATE
    final CbsaWageIndexEntry wageIndexEntry = lookupWageIndex(wageIndices, serviceDate);

    // RETURN ERROR IF WAGE INDEX = 0 OR NOT NUMERIC *AND* NOT ON RURAL FLOOR LOOKUP
    if (wageIndexEntry == null) {
      context.applyClaimReturnCode(ReturnCode.WAGE_INDEX_EQUALS_ZERO_51);
      return null;
    }

    return wageIndexEntry;
  }

  /**
   * LOCATE THE CBSA RECORD WITH THE APPROPRIATE EFFECTIVE DATE. WHEN FOUND, SELECT THE APPLICABLE
   * WAGE INDEX VALUE.
   *
   * <p>WAGE INDEX RECORD EFFECTIVE DATE MEETS THE CRITERIA:
   *
   * <pre>
   *  - MUST NOT BE AFTER THE LATEST WAGE INDEX EFFECTIVE DATE THAT CAN BE USED BY THE CLAIM
   *  - EXCEPT FOR INDIAN HEALTH PROVIDERS (CBSAS 98 AND 99), MUST BE WITHIN THE CLAIM'S CALENDAR
   *    YEAR (SEARCH STARTS AT THE MOST CURRENT RECORD / LATEST EFFECTIVE DATE FOR THE CBSA)
   * </pre>
   *
   * <p>(19210-WAGE-LOOKUP)
   */
  protected CbsaWageIndexEntry lookupWageIndex(
      NavigableMap<LocalDate, CbsaWageIndexEntry> wageIndices, LocalDate serviceDate) {

    // Logic change from COBOL:
    // Indian health providers (CBSAs 98 and 99) are now added to each annual file making the CBSA
    // lookup the same for all CBSAs.

    // Get the map entry closest but less than or equal to the given service date.
    final Map.Entry<LocalDate, CbsaWageIndexEntry> entry = wageIndices.floorEntry(serviceDate);

    // Return null if an entry does not exist for the service date.
    if (entry == null) {
      return null;
    }

    // Return wage index entry if effective date is between the calendar year of the service date.
    final CbsaWageIndexEntry wageIndexEntry = entry.getValue().copyBuilder().build();
    if (wageIndexEntry.getEffectiveDate().getYear() == serviceDate.getYear()) {
      return wageIndexEntry;
    }

    return null;
  }
}
