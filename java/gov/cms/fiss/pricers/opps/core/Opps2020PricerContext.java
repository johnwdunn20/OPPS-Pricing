package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.tables.DataTables;

/**
 * 2020 extension of the base OppsPricerContext. Since 2020 is the inaugural year of the OPPS pricer
 * it does not contain any additional elements. However, future years will need to override methods
 * that differ annually such as returning the calculation version.
 */
public class Opps2020PricerContext extends OppsPricerContext {

  public static final String CALCULATION_VERSION = "2020.3";

  public Opps2020PricerContext(
      OppsClaimPricingRequest input, OppsClaimPricingResponse output, DataTables dataTables) {
    super(input, output, dataTables);
  }

  @Override
  public String getCalculationVersion() {
    return CALCULATION_VERSION;
  }
}
