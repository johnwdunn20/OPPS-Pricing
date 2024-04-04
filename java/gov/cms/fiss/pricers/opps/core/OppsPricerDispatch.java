package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.common.api.InternalPricerException;
import gov.cms.fiss.pricers.common.api.YearNotImplementedException;
import gov.cms.fiss.pricers.common.application.ClaimProcessor;
import gov.cms.fiss.pricers.common.application.PricerDispatch;
import gov.cms.fiss.pricers.opps.OppsPricerConfiguration;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.tables.DataTables;

public class OppsPricerDispatch
    extends PricerDispatch<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerConfiguration> {
  public OppsPricerDispatch(OppsPricerConfiguration pricerConfiguration) {
    super(pricerConfiguration, o -> o.getReturnCodeData().getCode());
  }

  @Override
  protected void initializeReferences(OppsPricerConfiguration pricerConfiguration) {
    DataTables.loadDataTables(pricerConfiguration);

    // New pricer year: Must add a new case statement for each new pricer year and the corresponding
    // OppsRulePricer implementation.
    for (final int supportedYear : pricerConfiguration.getSupportedYears()) {
      switch (supportedYear) {
        case 2020:
          yearReference.register(
              supportedYear, Opps2020RulePricer.class, DataTables.forYear(supportedYear));

          break;
        case 2021:
          yearReference.register(
              supportedYear, Opps2021RulePricer.class, DataTables.forYear(supportedYear));

          break;

        case 2022:
          yearReference.register(
              supportedYear, Opps2022RulePricer.class, DataTables.forYear(supportedYear));

          break;

        case 2023:
          yearReference.register(
              supportedYear, Opps2023RulePricer.class, DataTables.forYear(supportedYear));

          break;

        case 2024:
          yearReference.register(
              supportedYear, Opps2024RulePricer.class, DataTables.forYear(supportedYear));

          break;
        default:
          break;
      }
    }
  }

  @Override
  protected ClaimProcessor<OppsClaimPricingRequest, OppsClaimPricingResponse> getProcessor(
      OppsClaimPricingRequest input) throws YearNotImplementedException, InternalPricerException {
    return yearReference.fromCalendarYear(
        input.getClaimData().getServiceFromDate(), "serviceFromDate");
  }

  @Override
  protected boolean isErrorOutput(OppsClaimPricingResponse output) {
    return Integer.parseInt(output.getReturnCodeData().getCode()) >= 50;
  }
}
