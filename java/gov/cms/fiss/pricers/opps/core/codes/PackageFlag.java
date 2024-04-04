// Generated by delombok at Mon Apr 01 15:56:10 UTC 2024
package gov.cms.fiss.pricers.opps.core.codes;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public enum PackageFlag {
  /**
   * Not packaged.
   */
  NOT_PACKAGED_0("0"), /**
   * Packaged service.
   */
  SERVICE_1("1"), /**
   * Packaged as part of partial hospitalization per diem or daily mental health service per diem.
   */
  PER_DIEM_2("2"), /**
   * Artificial charges for surgical procedures (submitted charges for surgical HCPCS &lt; $1.01).
   */
  ARTIFICIAL_SURGICAL_3("3"), /**
   * Packaged as part of drug administration APC payment.
   */
  DRUG_ADMINISTRATION_4("4"), /**
   * Packaged as part of FQHC encounter payment.
   */
  FQHC_ENCOUNTER_5("5"), /**
   * Packaged preventive service as part of FQHC encounter payment not subject to coinsurance
   * payment.
   */
  FQHC_NO_COINSURANCE_6("6");
  private final String flag;

  PackageFlag(String flag) {
    this.flag = flag;
  }

  public boolean is(String flag) {
    return StringUtils.equals(this.flag, flag);
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public String getFlag() {
    return this.flag;
  }
}
