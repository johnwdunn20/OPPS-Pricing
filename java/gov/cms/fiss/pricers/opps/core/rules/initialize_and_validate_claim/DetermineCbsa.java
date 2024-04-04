package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.api.OutpatientProviderData;
import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.api.v2.OppsPaymentData;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import java.time.LocalDate;
import org.apache.commons.lang3.StringUtils;

/**
 * DETERMINE WHICH CBSA TO USE &amp; WHETHER TO USE THE SPECIAL. WAGE INDEX IN THE PSF, AND SET THE
 * INPATIENT DAILY. COINSURANCE LIMIT (IN 2 PLACES; CHANGE EVERY JANUARY).
 */
public class DetermineCbsa
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {
  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final OutpatientProviderData providerData = calculationContext.getProviderData();
    final OppsPaymentData paymentData = calculationContext.getPaymentData();
    final String specialPaymentIndicator = providerData.getSpecialPaymentIndicator();
    final LocalDate serviceFromDate = calculationContext.getClaimData().getServiceFromDate();
    final String psfCbsa; // H-PSF-CBSA

    // IF L-PSF-SPEC-PYMT-IND = 'Y'
    //   MOVE L-PSF-WI-CBSA TO H-PSF-CBSA
    // ELSE
    //   IF L-PSF-SPEC-PYMT-IND = ' '
    //     MOVE L-PSF-GEO-CBSA TO H-PSF-CBSA
    //   ELSE
    //     IF L-PSF-SPEC-PYMT-IND = 'D'
    //       MOVE L-PSF-WI-CBSA TO H-PSF-CBSA
    //     ELSE
    //       *-------------------------------------------------------------*
    //       *   USE SPECIAL WAGE INDEX WHEN INDICATED                     *
    //       *   (PSF RECORD EFFECTIVE DATE MUST BE WITHIN THE CLAIM'S CY) *
    //       *   ADDED 10-23-2014 FOR CY 2015                              *
    //       *-------------------------------------------------------------*
    //       IF (L-PSF-SPEC-PYMT-IND = '1' OR '2') AND
    //           (L-PSF-EFFDT >= W-CY-BEGIN-DATE AND
    //            L-PSF-EFFDT <= W-CY-END-DATE)
    //         MOVE L-PSF-GEO-CBSA TO H-PSF-CBSA A-CBSA
    //         MOVE L-PSF-SPEC-WGIDX TO H-WINX
    //         MOVE 1408 TO H-IP-LIMIT
    //         GO TO 19100-INIT-EXIT
    //       ELSE
    //         MOVE  52  TO A-CLM-RTN-CODE
    //         GO TO 19100-INIT-EXIT.
    if (StringUtils.equalsAny(specialPaymentIndicator, "D", "Y")) {
      psfCbsa = providerData.getCbsaWageIndexLocation();
    } else if (StringUtils.isEmpty(StringUtils.trimToEmpty(specialPaymentIndicator))) {
      psfCbsa = providerData.getCbsaActualGeographicLocation();

    } else if (StringUtils.equalsAny(specialPaymentIndicator, "1", "2")
        && providerData.getEffectiveDate().getYear() == serviceFromDate.getYear()
        && providerData.getSpecialWageIndex() != null) {

      // USE SPECIAL WAGE INDEX WHEN INDICATED (PSF RECORD EFFECTIVE DATE MUST BE WITHIN THE
      // CLAIM'S CY)
      paymentData.setFinalCbsa(providerData.getCbsaActualGeographicLocation());
      calculationContext.setWageIndex(providerData.getSpecialWageIndex());

      return;
    } else {
      calculationContext.applyClaimReturnCode(ReturnCode.WAGE_INDEX_INVALID_RECLASSIFICATION_52);
      return;
    }

    //    IF H-PSF-CBSA = SPACE
    //      MOVE  52  TO A-CLM-RTN-CODE
    //      GO TO 19100-INIT-EXIT.
    if (psfCbsa == null || psfCbsa.trim().isEmpty()) {
      calculationContext.applyClaimReturnCode(ReturnCode.WAGE_INDEX_INVALID_RECLASSIFICATION_52);
      return;
    }

    paymentData.setFinalCbsa(psfCbsa);
  }
}
