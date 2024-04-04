package gov.cms.fiss.pricers.opps.resources;

import com.codahale.metrics.annotation.Timed;
import gov.cms.fiss.pricers.common.api.InternalPricerException;
import gov.cms.fiss.pricers.common.api.YearNotImplementedException;
import gov.cms.fiss.pricers.common.application.Rfc7807Support;
import gov.cms.fiss.pricers.common.application.filters.PricerContentLogFilter;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingRequest;
import gov.cms.fiss.pricers.opps.api.v2.OppsClaimPricingResponse;
import gov.cms.fiss.pricers.opps.core.OppsPricerDispatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.zalando.problem.spring.common.MediaTypes;

@Consumes(MediaType.APPLICATION_JSON)
@Path("/v2/price-claim")
@Produces({MediaType.APPLICATION_JSON, MediaTypes.PROBLEM_VALUE})
public class OppsClaimPricingResource {
  private final OppsPricerDispatch dispatch;

  public OppsClaimPricingResource(OppsPricerDispatch dispatch) {
    this.dispatch = dispatch;
  }

  @POST
  @Rfc7807Support
  @Timed
  @Operation(
      summary = "Prices an OPPS claim.",
      description =
          "Generates the pricing result for an OPPS Claim. The result includes payment data, as "
              + "well as details on how it was calculated and any potential adjustments made.",
      parameters = {
        @Parameter(
            in = ParameterIn.HEADER,
            name = PricerContentLogFilter.HEADER_REQUEST_ID,
            description = "The unique request identifier."),
        @Parameter(
            in = ParameterIn.HEADER,
            name = PricerContentLogFilter.HEADER_DCN,
            description = "The CMS document control number corresponding to the claim.")
      },
      responses = {
        @ApiResponse(
            headers = {
              @Header(
                  name = PricerContentLogFilter.HEADER_TRANSACTION_ID,
                  description = "A unique transaction identifier generated for claim processing."),
              @Header(
                  name = PricerContentLogFilter.HEADER_REQUEST_ID,
                  description = "The unique request identifier provided with the request."),
              @Header(
                  name = PricerContentLogFilter.HEADER_DCN,
                  description = "The CMS document control number provided with the request.")
            },
            responseCode = "200",
            description = "The pricing result for the claim.",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = OppsClaimPricingResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description =
                "Invalid input provided. Please correct the indicated issues and re-submit your"
                    + " request.",
            content =
                @Content(
                    mediaType = MediaTypes.PROBLEM_VALUE,
                    examples = {
                      @ExampleObject(
                          description = "The error related to the parsing failure.",
                          name = "jsonParsingFailure",
                          summary = "Invalid JSON",
                          value =
                              "\n"
                                  + "\n"
                                  + "{\n"
                                  + "  \"title\": \"Unable to process JSON\",\n"
                                  + "  \"status\": 400,\n"
                                  + "  \"detail\": \"Unexpected character ('x' (code 120)): was "
                                  + "expecting a colon to separate field name and value\"\n"
                                  + "}\n"
                                  + "\n"),
                      @ExampleObject(
                          description = "The validation error(s) found.",
                          name = "fieldValidationFailure",
                          summary = "Invalid request content",
                          value =
                              "{\n"
                                  + "  \"status\": 400,\n"
                                  + "  \"violations\": [\n"
                                  + "    {\n"
                                  + "      \"field\": \"priceClaim.arg0.claimData.providerCcn\",\n"
                                  + "      \"message\": \"must be six digits or uppercase letters\"\n"
                                  + "    }\n"
                                  + "  ],\n"
                                  + "  \"title\": \"Constraint Violation\"\n"
                                  + "}")
                    })),
        @ApiResponse(
            responseCode = "500",
            description =
                "Internal processing error. This is usually the result of an internal issue that "
                    + "the client cannot resolve.",
            content =
                @Content(
                    mediaType = MediaTypes.PROBLEM_VALUE,
                    examples = {
                      @ExampleObject(
                          description = "The error information.",
                          name = "internalServerError",
                          summary = "Internal processing error",
                          value =
                              "{\n"
                                  + "  \"title\": \"Internal Pricer Exception\",\n"
                                  + "  \"status\": 500,\n"
                                  + "  \"detail\": \"An unspecified exception occurred.\"\n"
                                  + "}")
                    }))
      })
  public OppsClaimPricingResponse priceClaim(
      @NotNull
          @Valid
          @Parameter(
              description = "The claim to be priced.",
              name = "pricingRequest",
              required = true,
              schema = @Schema(implementation = OppsClaimPricingRequest.class))
          OppsClaimPricingRequest pricingRequest)
      throws YearNotImplementedException, InternalPricerException {
    return dispatch.process(pricingRequest);
  }
}
