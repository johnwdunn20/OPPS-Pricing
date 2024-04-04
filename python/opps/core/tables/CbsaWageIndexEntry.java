// Generated by delombok at Mon Apr 01 15:56:10 UTC 2024
package gov.cms.fiss.pricers.opps.core.tables;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Pattern;

@Immutable
@JsonDeserialize(builder = CbsaWageIndexEntry.CbsaWageIndexEntryBuilder.class)
public class CbsaWageIndexEntry {

  @JsonPOJOBuilder(withPrefix = "")
  public static class CbsaWageIndexEntryBuilder {
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    private String cbsa;
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    private LocalDate effectiveDate;
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    private BigDecimal geographicWageIndex;
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    private BigDecimal imputedFloorWageIndex;
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    private String name;
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    private BigDecimal reclassifiedWageIndex;
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    private BigDecimal ruralFloorWageIndex;
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    private String size;

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    CbsaWageIndexEntryBuilder() {
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public CbsaWageIndexEntry.CbsaWageIndexEntryBuilder cbsa(final String cbsa) {
      this.cbsa = cbsa;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @JsonFormat(shape = Shape.STRING, pattern = "yyyyMMdd")
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public CbsaWageIndexEntry.CbsaWageIndexEntryBuilder effectiveDate(final LocalDate effectiveDate) {
      this.effectiveDate = effectiveDate;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public CbsaWageIndexEntry.CbsaWageIndexEntryBuilder geographicWageIndex(final BigDecimal geographicWageIndex) {
      this.geographicWageIndex = geographicWageIndex;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public CbsaWageIndexEntry.CbsaWageIndexEntryBuilder imputedFloorWageIndex(final BigDecimal imputedFloorWageIndex) {
      this.imputedFloorWageIndex = imputedFloorWageIndex;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public CbsaWageIndexEntry.CbsaWageIndexEntryBuilder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public CbsaWageIndexEntry.CbsaWageIndexEntryBuilder reclassifiedWageIndex(final BigDecimal reclassifiedWageIndex) {
      this.reclassifiedWageIndex = reclassifiedWageIndex;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public CbsaWageIndexEntry.CbsaWageIndexEntryBuilder ruralFloorWageIndex(final BigDecimal ruralFloorWageIndex) {
      this.ruralFloorWageIndex = ruralFloorWageIndex;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public CbsaWageIndexEntry.CbsaWageIndexEntryBuilder size(final String size) {
      this.size = size;
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public CbsaWageIndexEntry build() {
      return new CbsaWageIndexEntry(this.cbsa, this.effectiveDate, this.geographicWageIndex, this.imputedFloorWageIndex, this.name, this.reclassifiedWageIndex, this.ruralFloorWageIndex, this.size);
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public java.lang.String toString() {
      return "CbsaWageIndexEntry.CbsaWageIndexEntryBuilder(cbsa=" + this.cbsa + ", effectiveDate=" + this.effectiveDate + ", geographicWageIndex=" + this.geographicWageIndex + ", imputedFloorWageIndex=" + this.imputedFloorWageIndex + ", name=" + this.name + ", reclassifiedWageIndex=" + this.reclassifiedWageIndex + ", ruralFloorWageIndex=" + this.ruralFloorWageIndex + ", size=" + this.size + ")";
    }
  }

  @Pattern(regexp = "\\d{2}|\\d{5}", message = "must be either two or five digits")
  private final String cbsa;
  @JsonFormat(shape = Shape.STRING, pattern = "yyyyMMdd")
  private final LocalDate effectiveDate;
  @Digits(integer = 2, fraction = 4)
  private final BigDecimal geographicWageIndex; // winx1
  @Digits(integer = 2, fraction = 4)
  private final BigDecimal imputedFloorWageIndex; // winx4
  private final String name;
  @Digits(integer = 2, fraction = 4)
  private final BigDecimal reclassifiedWageIndex; // winx2
  @Digits(integer = 2, fraction = 4)
  private final BigDecimal ruralFloorWageIndex; // winx3
  @Pattern(regexp = "^([A-Z]|)$")
  private final String size;

  public CbsaWageIndexEntryBuilder copyBuilder() {
    return CbsaWageIndexEntry.builder().cbsa(getCbsa()).effectiveDate(getEffectiveDate()).geographicWageIndex(getGeographicWageIndex()).imputedFloorWageIndex(getImputedFloorWageIndex()).name(getName()).reclassifiedWageIndex(getReclassifiedWageIndex()).ruralFloorWageIndex(getRuralFloorWageIndex()).size(getSize());
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  CbsaWageIndexEntry(final String cbsa, final LocalDate effectiveDate, final BigDecimal geographicWageIndex, final BigDecimal imputedFloorWageIndex, final String name, final BigDecimal reclassifiedWageIndex, final BigDecimal ruralFloorWageIndex, final String size) {
    this.cbsa = cbsa;
    this.effectiveDate = effectiveDate;
    this.geographicWageIndex = geographicWageIndex;
    this.imputedFloorWageIndex = imputedFloorWageIndex;
    this.name = name;
    this.reclassifiedWageIndex = reclassifiedWageIndex;
    this.ruralFloorWageIndex = ruralFloorWageIndex;
    this.size = size;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public static CbsaWageIndexEntry.CbsaWageIndexEntryBuilder builder() {
    return new CbsaWageIndexEntry.CbsaWageIndexEntryBuilder();
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public String getCbsa() {
    return this.cbsa;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public LocalDate getEffectiveDate() {
    return this.effectiveDate;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getGeographicWageIndex() {
    return this.geographicWageIndex;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getImputedFloorWageIndex() {
    return this.imputedFloorWageIndex;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public String getName() {
    return this.name;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getReclassifiedWageIndex() {
    return this.reclassifiedWageIndex;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public BigDecimal getRuralFloorWageIndex() {
    return this.ruralFloorWageIndex;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public String getSize() {
    return this.size;
  }
}