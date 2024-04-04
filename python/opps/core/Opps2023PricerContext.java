package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.tables.DataTables;
import java.math.BigDecimal;

/** 2023 extension of the base OppsPricerContext. */
public class Opps2023PricerContext extends OppsPricerContext {

  public static final String CALCULATION_VERSION = "2023.2";

  public Opps2023PricerContext(
      OppsClaimPricingRequest input, OppsClaimPricingResponse output, DataTables dataTables) {
    super(input, output, dataTables);
  }

  @Override
  public String getCalculationVersion() {
    return CALCULATION_VERSION;
  }
  // 2023 CRT X-RAY Reduction is 10%
  @Override
  public BigDecimal getXRayCRTReduction() {
    return new BigDecimal("0.90");
  }

  @Override
  public BigDecimal getInpatientDeductibleCap() {
    return new BigDecimal("1600");
  }
  // Quality Adjustment Factor
  @Override
  public BigDecimal getApcQualityReduction() {
    return new BigDecimal("0.9807");
  }

  @Override
  public BigDecimal getWageIndexQuartile() {
    return new BigDecimal("0.8427");
  }
  // Outlier Threshold Amount of 8625 for CY2023
  @Override
  public BigDecimal getLinePaymentOutlierOffset() {
    return new BigDecimal("8625");
  }

  @Override
  // Payment Adjustment Flag '25' cap set to 15% for 2023
  public BigDecimal getColonialProcedureCap() {
    return new BigDecimal("0.15");
  }
}
