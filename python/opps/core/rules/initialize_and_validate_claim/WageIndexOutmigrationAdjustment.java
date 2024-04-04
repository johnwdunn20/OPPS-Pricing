package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.api.OutpatientProviderData;
import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimData;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.tables.OutMigrationAdjustmentEntry;
import org.apache.commons.lang3.StringUtils;

public class WageIndexOutmigrationAdjustment
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final OppsClaimData input = calculationContext.getInput().getClaimData();
    final OutpatientProviderData providerData = calculationContext.getProviderData();

    // DETERMINE OUTMIGRATION ADJUSTMENT BASED ON COUNTY CODE, IF
    // WAGE INDEX CBSA FIELD AND STANDARDIZED AMOUNT CBSA FIELD
    // BLANK, AND SPECIAL PYMT INDICATOR ARE BLANK APPLY ADJUSTMENT
    // IF NOT- DONT APPLY
    if (OppsPricerContext.isZeroValue(providerData.getCbsaWageIndexLocation())
        && OppsPricerContext.isZeroValue(providerData.getPaymentCbsa())
        && StringUtils.isEmpty(
            StringUtils.trimToEmpty(providerData.getSpecialPaymentIndicator()))) {
      // HLD-OUTM-ADJ
      final OutMigrationAdjustmentEntry outMigrationAdjustmentEntry =
          calculationContext
              .getDataTables()
              .getOutMigrationAdjustment(
                  Integer.parseInt(providerData.getCountyCode()), input.getServiceFromDate());

      if (outMigrationAdjustmentEntry == null) {
        return;
      }

      // COMPUTE H-WINX = H-WINX + HLD-OUTM-ADJ
      calculationContext.setWageIndex(
          calculationContext.getWageIndex().add(outMigrationAdjustmentEntry.getAdjustmentFactor()));
    }
  }
}
