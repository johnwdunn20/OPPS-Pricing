package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.tables.CbsaWageIndexEntry;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Determines the value of the wage index based on comparison to the imputed floor wage index.
 *
 * @since 2022
 */
public class WageIndexImputedFloorAdjustment
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final String stateCode = calculationContext.getProviderData().getStateCode();
    final LocalDate serviceDate = calculationContext.getClaimData().getServiceFromDate();

    // Search the CBSA table for the state code
    final NavigableMap<LocalDate, CbsaWageIndexEntry> wageIndices =
        calculationContext.getDataTables().getCbsaWageIndexEntries(stateCode);

    // State code not found in CBSA table, then exit
    if (wageIndices == null) {
      return;
    }
    // Get the map entry closest but less than or equal to the given service date.
    final Map.Entry<LocalDate, CbsaWageIndexEntry> entry = wageIndices.floorEntry(serviceDate);

    // Exit if an entry does not exist for the service date.
    if (entry == null) {
      return;
    }

    // Pull in entire cbsa line from cbsa csv table
    final CbsaWageIndexEntry cbsaWageIndexEntry = entry.getValue().copyBuilder().build();

    // Check imputed floor wage index is NOT NULL and imputed floor wage index is Greater than Zero
    // and Greater than current wage index captured to this point, Use imputed floor wage index
    // if greater than current wage index.
    if (cbsaWageIndexEntry.getImputedFloorWageIndex() != null
        && BigDecimalUtils.isGreaterThanZero(cbsaWageIndexEntry.getImputedFloorWageIndex())
        && BigDecimalUtils.isGreaterThan(
            cbsaWageIndexEntry.getImputedFloorWageIndex(), calculationContext.getWageIndex())) {

      calculationContext.setWageIndex(cbsaWageIndexEntry.getImputedFloorWageIndex());
    }
  }
}
