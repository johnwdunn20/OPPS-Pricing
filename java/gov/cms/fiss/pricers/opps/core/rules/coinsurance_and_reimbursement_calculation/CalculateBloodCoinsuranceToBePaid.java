package gov.cms.fiss.pricers.opps.core.rules.coinsurance_and_reimbursement_calculation;

import gov.cms.fiss.pricers.common.application.rules.CalculationRule;
import gov.cms.fiss.pricers.common.util.BigDecimalUtils;
import gov.cms.fiss.pricers.opps.core.CoinsuranceCapContext;
import gov.cms.fiss.pricers.opps.core.model.CoinsuranceCapEntry;
import gov.cms.fiss.pricers.opps.core.model.CoinsuranceCapValues;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculateBloodCoinsuranceToBePaid
    implements CalculationRule<CoinsuranceCapEntry, CoinsuranceCapValues, CoinsuranceCapContext> {

  /**
   * If blood was used, calculate best coinsurance value for that date
   *
   * <pre>
   * FOR DAYS OF SERVICE WITH BLOOD COINSURANCE, DETERMINE THE
   * % OF TOTAL BLOOD COINSURANCE THAT CAN BE PAID IN ADDITION
   * TO THE DAY'S MOST EXPENSIVE PROCEDURE/VISIT WAGE ADJUSTED
   * COINSURANCE WHILE KEEPING WITHIN THE INPATIENT DAILY
   * COINSURANCE LIMIT.
   *
   * WHEN H-RATIO = 0, NONE OF THE BLOOD COINSURANCE CAN BE PAID
   * WHEN H-RATIO = 1, ALL OF THE BLOOD COINSURANCE CAN BE PAID
   *
   * BY USING THE DAY'S MOST EXPENSIVE PROCEDURE/VIST WAGE
   * ADJUSTED COINSURANCE, THE BENEFICIARY RECEIVES THE
   * GREATEST BENEFIT FROM THE INPATIENT LIMITATION PROVISION.
   * </pre>
   *
   * <p>(19810-PROCESS-TYPE1)
   */
  @Override
  public void calculate(CoinsuranceCapContext calculationContext) {
    CoinsuranceCapEntry entry = calculationContext.getInput();
    CoinsuranceCapValues lastReturn = calculationContext.getOutput();

    if (entry.getCode() == 1) {
      // BLOOD WAS ADMINISTERED ON THE DAY
      if (BigDecimalUtils.isGreaterThanZero(entry.getCoinsurance2())) {
        // GET DATE OF SERVICE & ACTUAL COINSURANCE OF THE DAY'S MOST EXPENSIVE PROCEDURE/VISIT
        lastReturn.setLastCoinsuranceDateOfService(entry.getDateOfService());
        lastReturn.setTotal(entry.getCoinsurance1());

        // CALCULATE THE % OF THE DAY'S TOTAL BLOOD COIN THAT CAN BE PAID IN ADDITION TO THE
        // PROCEDURE/VISIT COIN WITHIN THE INPATIENT LIMIT
        // COMPUTE H-RATIO = (H-IP-LIMIT - W-DCP-COIN1 (W-DCP-INDX)) / W-DCP-COIN2 (W-DCP-INDX)
        lastReturn.setRatio(
            calculationContext
                .getPricerContext()
                .getInpatientDeductibleLimit()
                .subtract(entry.getCoinsurance1())
                .divide(entry.getCoinsurance2(), 7, RoundingMode.DOWN));
        // NONE OF THE DAY'S BLOOD COIN CAN BE PAID B/C THE PROCEDURE/VISIT COIN > INPATIENT COIN
        // LIMIT
        if (BigDecimalUtils.isLessThanZero(lastReturn.getRatio())) {
          lastReturn.setRatio(BigDecimalUtils.ZERO);
        }
      }

      // THE DAY'S TOTAL BLOOD COINSURANCE CAN BE PAID WITHIN THE INPATIENT COIN LIMIT
      if (BigDecimalUtils.isGreaterThan(lastReturn.getRatio(), BigDecimal.ONE)) {
        lastReturn.setRatio(BigDecimal.ONE);
      }
    }
  }
}
