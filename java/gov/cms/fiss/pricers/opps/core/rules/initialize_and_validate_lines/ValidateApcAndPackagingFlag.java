package gov.cms.fiss.pricers.opps.core.rules.initialize_and_validate_lines;

import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.core.ServiceLineContext;
import gov.cms.fiss.pricers.opps.core.codes.PackageFlag;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import java.util.stream.Stream;

public class ValidateApcAndPackagingFlag extends AbstractLineCalculationRule {
  private static final String NULL_CODE = "00000";

  @Override
  public void calculate(ServiceLineContext calculationContext) {
    // SET RETURN CODE & EXIT WHEN PACKAGED LINE/LINE APC = 0000
    final IoceServiceLineData serviceLineData = calculationContext.getInput();
    if (NULL_CODE.equals(serviceLineData.getPaymentApc())
        || isPackaged(serviceLineData.getPackageFlag())) {
      calculationContext.applyLineReturnCode(ReturnCode.INVALID_APC_OR_PACKAGING_FLAG_42);
    }
  }

  private boolean isPackaged(String packageFlag) {
    return Stream.of(
            PackageFlag.SERVICE_1, PackageFlag.PER_DIEM_2, PackageFlag.DRUG_ADMINISTRATION_4)
        .anyMatch(pf -> pf.is(packageFlag));
  }
}
