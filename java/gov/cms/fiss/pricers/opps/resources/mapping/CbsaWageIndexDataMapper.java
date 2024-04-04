package gov.cms.fiss.pricers.opps.resources.mapping;

import gov.cms.fiss.pricers.common.api.CbsaWageIndexData;
import gov.cms.fiss.pricers.opps.core.tables.CbsaWageIndexEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CbsaWageIndexDataMapper {
  CbsaWageIndexDataMapper INSTANCE = Mappers.getMapper(CbsaWageIndexDataMapper.class);

  @Mapping(target = "size", ignore = true)
  CbsaWageIndexData mapToCbsaWageIndexData(CbsaWageIndexEntry data);
}
