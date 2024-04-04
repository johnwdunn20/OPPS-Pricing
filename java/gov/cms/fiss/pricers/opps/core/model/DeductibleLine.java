// Generated by delombok at Mon Apr 01 15:56:10 UTC 2024
package gov.cms.fiss.pricers.opps.core.model;

import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Coinsurance/Blood Deductible table.
 *
 * <p>(W-LINE-PTR-TABLE) (W-BLOOD-PTR-TABLE)
 */
public class DeductibleLine {
  private static final BigDecimal ZERO = BigDecimalUtils.ZERO;
  private IoceServiceLineData serviceLine; // W-LP-SUB
  private BigDecimal nationalCoinsurance; // W-NAT-COIN
  private BigDecimal minimumCoinsurance; // W-MIN-COIN
  private BigDecimal subCharge; // W-SUB-CHRG
  private BigDecimal apcPayment; // W-APC-PYMT & W-BD-APC-PYMT
  private BigDecimal wageIndex; // W-WINX
  private Integer apcRank; // W-RANK
  private Integer bloodRank; // W-BD-RANK
  private BigDecimal reimbursementRate; // W-PPCT
  private BigDecimal discountRate; // W-DISC-RATE & W-BD-DISC-RATE
  private Integer serviceUnits = 0; // W-SRVC-UNITS & W-BD-SRVC-UNITS
  private BigDecimal reducedCoinsurance = ZERO; // W-RED-COIN
  private LocalDate dateOfService; // W-BD-DOS

  private DeductibleLine() {
  }

  public DeductibleLine(IoceServiceLineData serviceLineData, APCCalculationData apcData, BigDecimal apcPayment, BigDecimal wageIndex, BigDecimal discountRate) {
    setServiceLine(serviceLineData);
    setNationalCoinsurance(apcData.getNationalCoinsurance());
    setMinimumCoinsurance(apcData.getMinimumCoinsurance());
    setSubCharge(serviceLineData.getCoveredCharges());
    setApcPayment(apcPayment);
    setWageIndex(wageIndex);
    setApcRank(apcData.getRank());
    setReimbursementRate(apcData.getRate());
    setDiscountRate(discountRate);
    setServiceUnits(serviceLineData.getApcServiceUnits());
  }

  public DeductibleLine(DeductibleLine copyOf) {
    this.serviceLine = copyOf.getServiceLine();
    this.nationalCoinsurance = copyOf.getNationalCoinsurance();
    this.minimumCoinsurance = copyOf.getMinimumCoinsurance();
    this.subCharge = copyOf.getSubCharge();
    this.apcPayment = copyOf.getApcPayment();
    this.wageIndex = copyOf.getWageIndex();
    this.apcRank = copyOf.getApcRank();
    this.bloodRank = copyOf.getBloodRank();
    this.reimbursementRate = copyOf.getReimbursementRate();
    this.discountRate = copyOf.getDiscountRate();
    this.serviceUnits = copyOf.getServiceUnits();
    this.reducedCoinsurance = copyOf.getReducedCoinsurance();
    this.dateOfService = copyOf.getDateOfService();
  }

  // ORDER SERVICES LINES BY THE DEDUCTIBLE SEQUENCE -
  //   LOWEST TO HIGHEST APC RANK FROM APC TABLE
  //
  //   DEDUCTIBLE WILL BE TAKEN FROM OPPS SERVICES FIRST,
  //   THEN FROM ANY OTHER TYPES OF SERVICES FROM THE CLAIM.
  //   ALL VALID SERVICE LINES APPEAR IN THIS TABLE IN THE
  //   ORDER OF THEIR RANK FROM LOWEST TO HIGHEST.
  //     - THE LOWER THE RANK, THE HIGHER % THE NATIONAL
  //       UNADJUSTED COINSURANCE IS OF THE APC PAYMENT RATE
  //     - MOVE ALL LINE PRICING VARIABLES TO STAGING AREA
  //       (NEW COINSURANCE DEDUCTIBLE TABLE RECORD)
  //
  // NOTE: THE PURPOSE OF APC RANKING IS TO ENSURE THAT THE
  //       BENEFICIARY DEDUCTIBLE GOES TOWARD LINES WITH
  //       HIGHER COINSURANCE %S FIRST.  THIS RESULTS IN THE
  //       BENEFICIARY PAYING LESS TOTAL COINSURANCE FOR THE
  //       CLAIM.
  /**
   * Comparison method based on APC Rank.
   */
  public static int compareByApcRank(DeductibleLine line1, DeductibleLine line2) {
    return line1.getApcRank().compareTo(line2.getApcRank());
  }

  // POPULATE BLOOD DEDUCTIBLE TABLE W/ BLOOD DEDUC LINES
  // ORDERED EARLIEST TO LATEST DATE OF SERVICE AND THEN
  // LOWEST TO HIGHEST RANK FROM BLOOD DEDUCTIBLE TABLE
  // (APPLIES WHEN HCPCS IS IN BLOOD DEDUCTIBLE RANKING TABLE)
  /**
   * Comparison method based on Date of Service followed by Blood Rank.
   */
  public static int compareByDosThenBloodRank(DeductibleLine line1, DeductibleLine line2) {
    if (line1.getDateOfService().isEqual(line2.getDateOfService())) {
      return line1.getBloodRank() - line2.getBloodRank();
    }
    if (line1.getDateOfService().isBefore(line2.getDateOfService())) {
      return -1;
    }
    return 1;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public IoceServiceLineData getServiceLine() {
    return this.serviceLine;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getNationalCoinsurance() {
    return this.nationalCoinsurance;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getMinimumCoinsurance() {
    return this.minimumCoinsurance;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getSubCharge() {
    return this.subCharge;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getApcPayment() {
    return this.apcPayment;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getWageIndex() {
    return this.wageIndex;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public Integer getApcRank() {
    return this.apcRank;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public Integer getBloodRank() {
    return this.bloodRank;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getReimbursementRate() {
    return this.reimbursementRate;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getDiscountRate() {
    return this.discountRate;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public Integer getServiceUnits() {
    return this.serviceUnits;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getReducedCoinsurance() {
    return this.reducedCoinsurance;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public LocalDate getDateOfService() {
    return this.dateOfService;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setServiceLine(final IoceServiceLineData serviceLine) {
    this.serviceLine = serviceLine;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setNationalCoinsurance(final BigDecimal nationalCoinsurance) {
    this.nationalCoinsurance = nationalCoinsurance;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setMinimumCoinsurance(final BigDecimal minimumCoinsurance) {
    this.minimumCoinsurance = minimumCoinsurance;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setSubCharge(final BigDecimal subCharge) {
    this.subCharge = subCharge;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setApcPayment(final BigDecimal apcPayment) {
    this.apcPayment = apcPayment;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setWageIndex(final BigDecimal wageIndex) {
    this.wageIndex = wageIndex;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setApcRank(final Integer apcRank) {
    this.apcRank = apcRank;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setBloodRank(final Integer bloodRank) {
    this.bloodRank = bloodRank;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setReimbursementRate(final BigDecimal reimbursementRate) {
    this.reimbursementRate = reimbursementRate;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setDiscountRate(final BigDecimal discountRate) {
    this.discountRate = discountRate;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setServiceUnits(final Integer serviceUnits) {
    this.serviceUnits = serviceUnits;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setReducedCoinsurance(final BigDecimal reducedCoinsurance) {
    this.reducedCoinsurance = reducedCoinsurance;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public void setDateOfService(final LocalDate dateOfService) {
    this.dateOfService = dateOfService;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public boolean equals(final java.lang.Object o) {
    if (o == this) return true;
    if (!(o instanceof DeductibleLine)) return false;
    final DeductibleLine other = (DeductibleLine) o;
    if (!other.canEqual((java.lang.Object) this)) return false;
    final java.lang.Object this$apcRank = this.getApcRank();
    final java.lang.Object other$apcRank = other.getApcRank();
    if (this$apcRank == null ? other$apcRank != null : !this$apcRank.equals(other$apcRank)) return false;
    final java.lang.Object this$bloodRank = this.getBloodRank();
    final java.lang.Object other$bloodRank = other.getBloodRank();
    if (this$bloodRank == null ? other$bloodRank != null : !this$bloodRank.equals(other$bloodRank)) return false;
    final java.lang.Object this$serviceUnits = this.getServiceUnits();
    final java.lang.Object other$serviceUnits = other.getServiceUnits();
    if (this$serviceUnits == null ? other$serviceUnits != null : !this$serviceUnits.equals(other$serviceUnits)) return false;
    final java.lang.Object this$serviceLine = this.getServiceLine();
    final java.lang.Object other$serviceLine = other.getServiceLine();
    if (this$serviceLine == null ? other$serviceLine != null : !this$serviceLine.equals(other$serviceLine)) return false;
    final java.lang.Object this$nationalCoinsurance = this.getNationalCoinsurance();
    final java.lang.Object other$nationalCoinsurance = other.getNationalCoinsurance();
    if (this$nationalCoinsurance == null ? other$nationalCoinsurance != null : !this$nationalCoinsurance.equals(other$nationalCoinsurance)) return false;
    final java.lang.Object this$minimumCoinsurance = this.getMinimumCoinsurance();
    final java.lang.Object other$minimumCoinsurance = other.getMinimumCoinsurance();
    if (this$minimumCoinsurance == null ? other$minimumCoinsurance != null : !this$minimumCoinsurance.equals(other$minimumCoinsurance)) return false;
    final java.lang.Object this$subCharge = this.getSubCharge();
    final java.lang.Object other$subCharge = other.getSubCharge();
    if (this$subCharge == null ? other$subCharge != null : !this$subCharge.equals(other$subCharge)) return false;
    final java.lang.Object this$apcPayment = this.getApcPayment();
    final java.lang.Object other$apcPayment = other.getApcPayment();
    if (this$apcPayment == null ? other$apcPayment != null : !this$apcPayment.equals(other$apcPayment)) return false;
    final java.lang.Object this$wageIndex = this.getWageIndex();
    final java.lang.Object other$wageIndex = other.getWageIndex();
    if (this$wageIndex == null ? other$wageIndex != null : !this$wageIndex.equals(other$wageIndex)) return false;
    final java.lang.Object this$reimbursementRate = this.getReimbursementRate();
    final java.lang.Object other$reimbursementRate = other.getReimbursementRate();
    if (this$reimbursementRate == null ? other$reimbursementRate != null : !this$reimbursementRate.equals(other$reimbursementRate)) return false;
    final java.lang.Object this$discountRate = this.getDiscountRate();
    final java.lang.Object other$discountRate = other.getDiscountRate();
    if (this$discountRate == null ? other$discountRate != null : !this$discountRate.equals(other$discountRate)) return false;
    final java.lang.Object this$reducedCoinsurance = this.getReducedCoinsurance();
    final java.lang.Object other$reducedCoinsurance = other.getReducedCoinsurance();
    if (this$reducedCoinsurance == null ? other$reducedCoinsurance != null : !this$reducedCoinsurance.equals(other$reducedCoinsurance)) return false;
    final java.lang.Object this$dateOfService = this.getDateOfService();
    final java.lang.Object other$dateOfService = other.getDateOfService();
    if (this$dateOfService == null ? other$dateOfService != null : !this$dateOfService.equals(other$dateOfService)) return false;
    return true;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  protected boolean canEqual(final java.lang.Object other) {
    return other instanceof DeductibleLine;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final java.lang.Object $apcRank = this.getApcRank();
    result = result * PRIME + ($apcRank == null ? 43 : $apcRank.hashCode());
    final java.lang.Object $bloodRank = this.getBloodRank();
    result = result * PRIME + ($bloodRank == null ? 43 : $bloodRank.hashCode());
    final java.lang.Object $serviceUnits = this.getServiceUnits();
    result = result * PRIME + ($serviceUnits == null ? 43 : $serviceUnits.hashCode());
    final java.lang.Object $serviceLine = this.getServiceLine();
    result = result * PRIME + ($serviceLine == null ? 43 : $serviceLine.hashCode());
    final java.lang.Object $nationalCoinsurance = this.getNationalCoinsurance();
    result = result * PRIME + ($nationalCoinsurance == null ? 43 : $nationalCoinsurance.hashCode());
    final java.lang.Object $minimumCoinsurance = this.getMinimumCoinsurance();
    result = result * PRIME + ($minimumCoinsurance == null ? 43 : $minimumCoinsurance.hashCode());
    final java.lang.Object $subCharge = this.getSubCharge();
    result = result * PRIME + ($subCharge == null ? 43 : $subCharge.hashCode());
    final java.lang.Object $apcPayment = this.getApcPayment();
    result = result * PRIME + ($apcPayment == null ? 43 : $apcPayment.hashCode());
    final java.lang.Object $wageIndex = this.getWageIndex();
    result = result * PRIME + ($wageIndex == null ? 43 : $wageIndex.hashCode());
    final java.lang.Object $reimbursementRate = this.getReimbursementRate();
    result = result * PRIME + ($reimbursementRate == null ? 43 : $reimbursementRate.hashCode());
    final java.lang.Object $discountRate = this.getDiscountRate();
    result = result * PRIME + ($discountRate == null ? 43 : $discountRate.hashCode());
    final java.lang.Object $reducedCoinsurance = this.getReducedCoinsurance();
    result = result * PRIME + ($reducedCoinsurance == null ? 43 : $reducedCoinsurance.hashCode());
    final java.lang.Object $dateOfService = this.getDateOfService();
    result = result * PRIME + ($dateOfService == null ? 43 : $dateOfService.hashCode());
    return result;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public java.lang.String toString() {
    return "DeductibleLine(serviceLine=" + this.getServiceLine() + ", nationalCoinsurance=" + this.getNationalCoinsurance() + ", minimumCoinsurance=" + this.getMinimumCoinsurance() + ", subCharge=" + this.getSubCharge() + ", apcPayment=" + this.getApcPayment() + ", wageIndex=" + this.getWageIndex() + ", apcRank=" + this.getApcRank() + ", bloodRank=" + this.getBloodRank() + ", reimbursementRate=" + this.getReimbursementRate() + ", discountRate=" + this.getDiscountRate() + ", serviceUnits=" + this.getServiceUnits() + ", reducedCoinsurance=" + this.getReducedCoinsurance() + ", dateOfService=" + this.getDateOfService() + ")";
  }
}