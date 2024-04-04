package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.tables.DataTables;
import java.math.BigDecimal;

/** 2022 extension of the base OppsPricerContext. */
public class Opps2022PricerContext extends OppsPricerContext {

  public static final String CALCULATION_VERSION = "2022.2";

  public Opps2022PricerContext(
      OppsClaimPricingRequest input, OppsClaimPricingResponse output, DataTables dataTables) {
    super(input, output, dataTables);
  }

  @Override
  public String getCalculationVersion() {
    return CALCULATION_VERSION;
  }

  @Override
  public BigDecimal getInpatientDeductibleCap() {
    return new BigDecimal("1556");
  }
  // Quality Adjustment Factor
  @Override
  public BigDecimal getApcQualityReduction() {
    return new BigDecimal("0.9804");
  }

  @Override
  public BigDecimal getWageIndexQuartile() {
    return new BigDecimal("0.8437");
  }
  // Outlier Threshold Amount of 6175 for CY2022
  @Override
  public BigDecimal getLinePaymentOutlierOffset() {
    return new BigDecimal("6175");
  }

  @Override
  // Payment Adjustment Flag '25' cap set to 20% for 2022
  public BigDecimal getColonialProcedureCap() {
    return new BigDecimal("0.20");
  }
}
