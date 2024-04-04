package gov.cms.fiss.pricers.opps.core.tables;

import gov.cms.fiss.pricers.common.csv.CsvContentReader;
import gov.cms.fiss.pricers.common.csv.CsvIngestionConfiguration;
import gov.cms.fiss.pricers.common.csv.LookupGenerator;
import java.util.List;
import java.util.Map;

public class PreviousYearWageIndexLookupGenerator {

  private static final String CSV_FILE_PATTERN = "/previous-year-wage-index-2019.csv";

  private final CsvContentReader<PreviousYearWageIndexEntry> contentProvider =
      new CsvContentReader<>(PreviousYearWageIndexEntry.class)
          .customizeSchema(CsvContentReader.HEADER_ROW_CUSTOMIZER);

  public PreviousYearWageIndexLookupGenerator(CsvIngestionConfiguration csvIngestionConfiguration) {
    if (!csvIngestionConfiguration.isValidationEnabled()) {
      contentProvider.disableValidation();
    }
  }

  /**
   * Creates a lookup table for previous wage index data
   *
   * @return the populated table
   */
  public Map<String, PreviousYearWageIndexEntry> generate() {
    return generate(CSV_FILE_PATTERN);
  }

  /**
   * Creates a lookup table for previous wage index data with the given CSV file name
   *
   * @return the populated table
   */
  public Map<String, PreviousYearWageIndexEntry> generate(String csvFile) {
    final List<PreviousYearWageIndexEntry> content = contentProvider.read(csvFile);

    return LookupGenerator.generateMap(PreviousYearWageIndexEntry::getProviderNumber, content);
  }
}
