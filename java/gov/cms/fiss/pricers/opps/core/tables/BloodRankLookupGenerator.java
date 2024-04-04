package gov.cms.fiss.pricers.opps.core.tables;

import gov.cms.fiss.pricers.common.csv.CsvContentReader;
import gov.cms.fiss.pricers.common.csv.CsvIngestionConfiguration;
import gov.cms.fiss.pricers.common.csv.LookupGenerator;
import java.util.List;
import java.util.Map;

public class BloodRankLookupGenerator {

  private static final String CSV_FILE_PATTERN = "/blood-rank-%s.csv";

  private final CsvContentReader<BloodRankEntry> contentProvider =
      new CsvContentReader<>(BloodRankEntry.class)
          .customizeSchema(CsvContentReader.HEADER_ROW_CUSTOMIZER);

  public BloodRankLookupGenerator(CsvIngestionConfiguration csvIngestionConfiguration) {
    if (!csvIngestionConfiguration.isValidationEnabled()) {
      contentProvider.disableValidation();
    }
  }

  /**
   * Creates a lookup table for Blood Ranking data for the given pricer year
   *
   * @param pricerYear the year to reference
   * @return the populated table
   */
  public Map<String, BloodRankEntry> generate(int pricerYear) {
    return generate(String.format(CSV_FILE_PATTERN, pricerYear));
  }

  /**
   * Creates a lookup table for Blood Ranking data with the given CSV file name
   *
   * @return the populated table
   */
  public Map<String, BloodRankEntry> generate(String csvFile) {
    final List<BloodRankEntry> content = contentProvider.read(csvFile);
    return LookupGenerator.generateMap(BloodRankEntry::getHcpcsCode, content);
  }
}
