package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.tables.DataTables;
import java.math.BigDecimal;

/** 2021 extension of the base OppsPricerContext. */
public class Opps2021PricerContext extends OppsPricerContext {

  public static final String CALCULATION_VERSION = "2021.4";

  public Opps2021PricerContext(
      OppsClaimPricingRequest input, OppsClaimPricingResponse output, DataTables dataTables) {
    super(input, output, dataTables);
  }

  @Override
  public String getCalculationVersion() {
    return CALCULATION_VERSION;
  }

  @Override
  public BigDecimal getInpatientDeductibleCap() {
    return new BigDecimal("1484");
  }
  // Quality Adjustment Factor
  @Override
  public BigDecimal getApcQualityReduction() {
    return new BigDecimal("0.9805");
  }

  @Override
  public BigDecimal getWageIndexQuartile() {
    return new BigDecimal("0.8469");
  }
  // Outlier Threshold Amount of 5300
  @Override
  public BigDecimal getLinePaymentOutlierOffset() {
    return new BigDecimal("5300");
  }
}
