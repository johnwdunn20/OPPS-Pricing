// Generated by delombok at Mon Apr 01 15:56:10 UTC 2024
package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import java.math.BigDecimal;

public class OutlierPaymentInfo {
  private static final BigDecimal ZERO = BigDecimalUtils.ZERO;
  private BigDecimal cmhcTotalPayment = ZERO; // H-CMHC-PYMT-TOT
  private BigDecimal cmhcTotalOutlier = ZERO; // H-CMHC-OUTL-TOT
  private BigDecimal outlierPayment = ZERO; // H-OUTL-PYMT

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getCmhcTotalPayment() {
    return this.cmhcTotalPayment;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getCmhcTotalOutlier() {
    return this.cmhcTotalOutlier;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getOutlierPayment() {
    return this.outlierPayment;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setCmhcTotalPayment(final BigDecimal cmhcTotalPayment) {
    this.cmhcTotalPayment = cmhcTotalPayment;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setCmhcTotalOutlier(final BigDecimal cmhcTotalOutlier) {
    this.cmhcTotalOutlier = cmhcTotalOutlier;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setOutlierPayment(final BigDecimal outlierPayment) {
    this.outlierPayment = outlierPayment;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public java.lang.String toString() {
    return "OutlierPaymentInfo(cmhcTotalPayment=" + this.getCmhcTotalPayment() + ", cmhcTotalOutlier=" + this.getCmhcTotalOutlier() + ", outlierPayment=" + this.getOutlierPayment() + ")";
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public OutlierPaymentInfo() {
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public OutlierPaymentInfo(final BigDecimal cmhcTotalPayment, final BigDecimal cmhcTotalOutlier, final BigDecimal outlierPayment) {
    this.cmhcTotalPayment = cmhcTotalPayment;
    this.cmhcTotalOutlier = cmhcTotalOutlier;
    this.outlierPayment = outlierPayment;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public boolean equals(final java.lang.Object o) {
    if (o == this) return true;
    if (!(o instanceof OutlierPaymentInfo)) return false;
    final OutlierPaymentInfo other = (OutlierPaymentInfo) o;
    if (!other.canEqual((java.lang.Object) this)) return false;
    final java.lang.Object this$cmhcTotalPayment = this.getCmhcTotalPayment();
    final java.lang.Object other$cmhcTotalPayment = other.getCmhcTotalPayment();
    if (this$cmhcTotalPayment == null ? other$cmhcTotalPayment != null : !this$cmhcTotalPayment.equals(other$cmhcTotalPayment)) return false;
    final java.lang.Object this$cmhcTotalOutlier = this.getCmhcTotalOutlier();
    final java.lang.Object other$cmhcTotalOutlier = other.getCmhcTotalOutlier();
    if (this$cmhcTotalOutlier == null ? other$cmhcTotalOutlier != null : !this$cmhcTotalOutlier.equals(other$cmhcTotalOutlier)) return false;
    final java.lang.Object this$outlierPayment = this.getOutlierPayment();
    final java.lang.Object other$outlierPayment = other.getOutlierPayment();
    if (this$outlierPayment == null ? other$outlierPayment != null : !this$outlierPayment.equals(other$outlierPayment)) return false;
    return true;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  protected boolean canEqual(final java.lang.Object other) {
    return other instanceof OutlierPaymentInfo;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final java.lang.Object $cmhcTotalPayment = this.getCmhcTotalPayment();
    result = result * PRIME + ($cmhcTotalPayment == null ? 43 : $cmhcTotalPayment.hashCode());
    final java.lang.Object $cmhcTotalOutlier = this.getCmhcTotalOutlier();
    result = result * PRIME + ($cmhcTotalOutlier == null ? 43 : $cmhcTotalOutlier.hashCode());
    final java.lang.Object $outlierPayment = this.getOutlierPayment();
    result = result * PRIME + ($outlierPayment == null ? 43 : $outlierPayment.hashCode());
    return result;
  }
}
