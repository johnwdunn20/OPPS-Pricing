// Generated by delombok at Mon Apr 01 15:56:10 UTC 2024
package gov.cms.fiss.pricers.opps.core.model;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CoinsuranceCapValues {
  private BigDecimal ratio = BigDecimalUtils.ZERO; // H-RATIO
  private BigDecimal total = BigDecimalUtils.ZERO; // H-TOTAL
  private LocalDate lastCoinsuranceDateOfService; // H-DCP-DOS

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public CoinsuranceCapValues() {
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getRatio() {
    return this.ratio;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getTotal() {
    return this.total;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public LocalDate getLastCoinsuranceDateOfService() {
    return this.lastCoinsuranceDateOfService;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setRatio(final BigDecimal ratio) {
    this.ratio = ratio;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setTotal(final BigDecimal total) {
    this.total = total;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setLastCoinsuranceDateOfService(final LocalDate lastCoinsuranceDateOfService) {
    this.lastCoinsuranceDateOfService = lastCoinsuranceDateOfService;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public boolean equals(final java.lang.Object o) {
    if (o == this) return true;
    if (!(o instanceof CoinsuranceCapValues)) return false;
    final CoinsuranceCapValues other = (CoinsuranceCapValues) o;
    if (!other.canEqual((java.lang.Object) this)) return false;
    final java.lang.Object this$ratio = this.getRatio();
    final java.lang.Object other$ratio = other.getRatio();
    if (this$ratio == null ? other$ratio != null : !this$ratio.equals(other$ratio)) return false;
    final java.lang.Object this$total = this.getTotal();
    final java.lang.Object other$total = other.getTotal();
    if (this$total == null ? other$total != null : !this$total.equals(other$total)) return false;
    final java.lang.Object this$lastCoinsuranceDateOfService = this.getLastCoinsuranceDateOfService();
    final java.lang.Object other$lastCoinsuranceDateOfService = other.getLastCoinsuranceDateOfService();
    if (this$lastCoinsuranceDateOfService == null ? other$lastCoinsuranceDateOfService != null : !this$lastCoinsuranceDateOfService.equals(other$lastCoinsuranceDateOfService)) return false;
    return true;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  protected boolean canEqual(final java.lang.Object other) {
    return other instanceof CoinsuranceCapValues;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final java.lang.Object $ratio = this.getRatio();
    result = result * PRIME + ($ratio == null ? 43 : $ratio.hashCode());
    final java.lang.Object $total = this.getTotal();
    result = result * PRIME + ($total == null ? 43 : $total.hashCode());
    final java.lang.Object $lastCoinsuranceDateOfService = this.getLastCoinsuranceDateOfService();
    result = result * PRIME + ($lastCoinsuranceDateOfService == null ? 43 : $lastCoinsuranceDateOfService.hashCode());
    return result;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public java.lang.String toString() {
    return "CoinsuranceCapValues(ratio=" + this.getRatio() + ", total=" + this.getTotal() + ", lastCoinsuranceDateOfService=" + this.getLastCoinsuranceDateOfService() + ")";
  }
}