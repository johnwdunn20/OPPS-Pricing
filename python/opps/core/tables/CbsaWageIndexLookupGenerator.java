package gov.cms.fiss.pricers.opps.core.tables;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser.Feature;
import gov.cms.fiss.pricers.common.csv.CsvContentReader;
import gov.cms.fiss.pricers.common.csv.CsvIngestionConfiguration;
import gov.cms.fiss.pricers.common.csv.LookupGenerator;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CbsaWageIndexLookupGenerator {

  private static final String CSV_FILE_PATTERN = "/cbsa-wage-index-%s.csv";

  private final CsvContentReader<CbsaWageIndexEntry> contentProvider =
      new CsvContentReader<>(CbsaWageIndexEntry.class)
          .customizeSchema(schema -> schema.withHeader().withColumnReordering(true));

  public CbsaWageIndexLookupGenerator(CsvIngestionConfiguration csvIngestionConfiguration) {
    if (!csvIngestionConfiguration.isValidationEnabled()) {
      contentProvider.disableValidation();
    }

    contentProvider.customizeMapper(
        mapper ->
            (CsvMapper)
                mapper
                    .disable(Feature.FAIL_ON_MISSING_COLUMNS)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
  }

  /**
   * Creates a lookup table for CBSA Wage Index data for the given pricer year.
   *
   * @param pricerYear the pricer year for the claim
   * @return the populated table
   */
  public Map<String, NavigableMap<LocalDate, CbsaWageIndexEntry>> generate(int pricerYear) {
    return generate(String.format(CSV_FILE_PATTERN, pricerYear));
  }

  /**
   * Creates a lookup table for CBSA Wage Index data with the given CSV file name.
   *
   * @param csvFile filename of the csv file to use
   * @return the populated table
   */
  public Map<String, NavigableMap<LocalDate, CbsaWageIndexEntry>> generate(String csvFile) {
    final List<CbsaWageIndexEntry> content = contentProvider.read(csvFile);
    final BinaryOperator<CbsaWageIndexEntry> comparator = BinaryOperator.maxBy((a, b) -> 0);
    final Collector<CbsaWageIndexEntry, ?, NavigableMap<LocalDate, CbsaWageIndexEntry>> collector =
        Collectors.toMap(
            CbsaWageIndexEntry::getEffectiveDate, Function.identity(), comparator, TreeMap::new);

    return LookupGenerator.generateCrossReferenceToMap(
        CbsaWageIndexEntry::getCbsa, content, HashMap::new, collector);
  }
}
