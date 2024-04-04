package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.tables.DataTables;
import java.math.BigDecimal;

/** 2024 extension of the base OppsPricerContext. */
public class Opps2024PricerContext extends OppsPricerContext {

  public static final String CALCULATION_VERSION = "2024.1";

  public Opps2024PricerContext(
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
    return new BigDecimal("1632");
  }
  // Quality Adjustment Factor
  @Override
  public BigDecimal getApcQualityReduction() {
    return new BigDecimal("0.9806");
  }

  @Override
  public BigDecimal getWageIndexQuartile() {
    return new BigDecimal("0.8667");
  }
  // Outlier Threshold Amount of 8125 for CY2024
  @Override
  public BigDecimal getLinePaymentOutlierOffset() {
    return new BigDecimal("7750");
  }

  @Override
  // Payment Adjustment Flag '25' cap set to 15% for 2023
  public BigDecimal getColonialProcedureCap() {
    return new BigDecimal("0.15");
  }
}
