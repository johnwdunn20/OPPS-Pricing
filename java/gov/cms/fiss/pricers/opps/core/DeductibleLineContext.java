// Generated by delombok at Mon Apr 01 15:56:10 UTC 2024
package gov.cms.fiss.pricers.opps.core;

import gov.cms.fiss.pricers.common.application.rules.CalculationContext;
import gov.cms.fiss.pricers.opps.api.v2.IoceServiceLineData;
import gov.cms.fiss.pricers.opps.api.v2.ServiceLinePaymentData;
import gov.cms.fiss.pricers.opps.core.codes.ReturnCode;
import gov.cms.fiss.pricers.opps.core.model.DeductibleLine;
import gov.cms.fiss.pricers.opps.core.model.LineCalculation;

/**
 * Context for processing individual deductible lines.
 */
public class DeductibleLineContext extends CalculationContext<DeductibleLine, ServiceLinePaymentData> {
  private final OppsPricerContext pricerContext;
  private final LineCalculation lineCalculation;
  private final OutlierPaymentInfo outlierPaymentInfo;
  private boolean standardPaymentCalculationCompleted;

  public DeductibleLineContext(OppsPricerContext pricerContext, DeductibleLine input) {
    this(pricerContext, input, new OutlierPaymentInfo());
  }

  public DeductibleLineContext(OppsPricerContext pricerContext, DeductibleLine input, OutlierPaymentInfo outlierPaymentInfo) {
    super(input, pricerContext.getServiceLinePaymentByLineNumber(input.getServiceLine().getLineNumber()));
    this.pricerContext = pricerContext;
    this.lineCalculation = new LineCalculation(input.getServiceLine(), this.getOutput(), input);
    this.outlierPaymentInfo = outlierPaymentInfo;
  }

  /**
   * Updates the line return code value for the corresponding output line.
   */
  public void applyLineReturnCode(ReturnCode returnCode) {
    setCalculationCompleted();
    getOutput().setReturnCode(returnCode.toReturnCodeData());
  }

  public boolean isStandardPaymentCalculationCompleted() {
    return standardPaymentCalculationCompleted;
  }

  public void completeStandardPaymentCalculation() {
    this.standardPaymentCalculationCompleted = true;
  }

  /**
   * Determines if line is a packaged blood deductible line.
   *
   * <pre>
   *   FLAG LINE IF ELIGIBLE FOR THE BLOOD DEDUCTIBLE AND CLAIM
   *   HAS A COMPREHENSIVE APC; TREAT AS PACKAGED LINE
   *   BUT LINE PAYMENT WILL REFLECT THE DEDUCTIBLE DUE
   * </pre>
   */
  public boolean isPackagedBloodDeductibleLine() {
    final IoceServiceLineData lineInput = getInput().getServiceLine();
    return OppsPricerContext.isComprehensiveBloodDeductible(lineInput.getStatusIndicator(), lineInput.getPaymentAdjustmentFlags(), getPricerContext().getComprehensiveApcClaimStatus()) && getPricerContext().getDataTables().isBloodHcpcsDeductible(lineInput.getHcpcsCode());
  }

  /**
   * Returns the status indicator from the service line.
   */
  public String getStatusIndicator() {
    final IoceServiceLineData ioceServiceLine = getLineCalculation().getLineInput();
    return ioceServiceLine.getStatusIndicator();
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public OppsPricerContext getPricerContext() {
    return this.pricerContext;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public LineCalculation getLineCalculation() {
    return this.lineCalculation;
  }

  @java.lang.SuppressWarnings("all")
  @lombok.Generated
  public OutlierPaymentInfo getOutlierPaymentInfo() {
    return this.outlierPaymentInfo;
  }
}
