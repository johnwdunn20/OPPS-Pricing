package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_claim;

import gov.cms.fiss.pricers.common.api.OutpatientProviderData;
import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimData;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerContext;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import java.time.LocalDate;
import org.apache.commons.lang3.StringUtils;

public class SetInitialValues
    implements CalculationRule<
        OppsClaimPricingRequest, OppsClaimPricingResponse, OppsPricerContext> {

  @Override
  public void calculate(OppsPricerContext calculationContext) {
    final OppsClaimData claimData = calculationContext.getClaimData();
    final OutpatientProviderData providerData = calculationContext.getProviderData();

    // UPDATE CAL-VERSION EVERY JANUARY
    calculationContext
        .getOutput()
        .setCalculationVersion(calculationContext.getCalculationVersion());

    // VALIDATE CLAIM & PSF DATES
    if (isBadServiceDate(claimData.getServiceFromDate(), providerData)) {
      calculationContext.applyClaimReturnCode(ReturnCode.SERVICE_FROM_DATE_NOT_IN_RANGE_54);
      return;
    }

    // Trim payment CBSA
    providerData.setPaymentCbsa(StringUtils.trimToEmpty(providerData.getPaymentCbsa()));

    // RECEIVE BENEFICIARY SPECIFIC ITEMS FROM THE OCE
    calculationContext.setBeneficiaryDeductible(claimData.getPatientDeductible());
    calculationContext.setBeneficiaryBloodPintsUsed(claimData.getBloodPintsRemaining());
  }

  /**
   * Returns true if the service date is before the provider's effective date or, if applicable, the
   * service date is after the provider's termination date.
   */
  protected boolean isBadServiceDate(LocalDate serviceDate, OutpatientProviderData providerData) {
    final LocalDate terminationDate = providerData.getTerminationDate();
    return serviceDate.isBefore(providerData.getEffectiveDate())
        || (terminationDate != null && serviceDate.isAfter(terminationDate));
  }
}
