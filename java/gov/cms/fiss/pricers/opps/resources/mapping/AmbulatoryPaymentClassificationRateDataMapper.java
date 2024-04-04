package gov.cms.fiss.pricers.opps.resources.mapping;

import gov.cms.fiss.pricers.common.api.AmbulatoryPaymentClassificationRateData;
import gov.cms.fiss.pricers.opps.core.tables.ApcRateHistoryEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AmbulatoryPaymentClassificationRateDataMapper {
  AmbulatoryPaymentClassificationRateDataMapper INSTANCE =
      Mappers.getMapper(AmbulatoryPaymentClassificationRateDataMapper.class);

  @Mapping(source = "apc", target = "ambulatoryPaymentClassificationCode")
  @Mapping(source = "nationalCoinsurance", target = "nationalCoinsuranceRate")
  @Mapping(source = "minimumCoinsurance", target = "minimumCoinsuranceRate")
  AmbulatoryPaymentClassificationRateData mapToAmbulatoryPaymentClassificationRateData(
      ApcRateHistoryEntry data);
}
