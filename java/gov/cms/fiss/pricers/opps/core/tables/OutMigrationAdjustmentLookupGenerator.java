package gov.cms.fiss.pricers.opps.core.tables;

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

public class OutMigrationAdjustmentLookupGenerator {

  private static final String CSV_FILE_PATTERN = "/outmigration-adjustment-%s.csv";

  private final CsvContentReader<OutMigrationAdjustmentEntry> contentProvider =
      new CsvContentReader<>(OutMigrationAdjustmentEntry.class)
          .customizeSchema(CsvContentReader.HEADER_ROW_CUSTOMIZER);

  public OutMigrationAdjustmentLookupGenerator(
      CsvIngestionConfiguration csvIngestionConfiguration) {
    if (!csvIngestionConfiguration.isValidationEnabled()) {
      contentProvider.disableValidation();
    }
  }

  /**
   * Creates a lookup table for out migration adjustment data for the given pricer year.
   *
   * @return the populated table
   */
  public Map<Integer, NavigableMap<LocalDate, OutMigrationAdjustmentEntry>> generate(
      int pricerYear) {
    return generate(String.format(CSV_FILE_PATTERN, pricerYear));
  }

  /**
   * Creates a lookup table for out migration adjustment data based on the given CSV file name.
   *
   * @return the populated table
   */
  public Map<Integer, NavigableMap<LocalDate, OutMigrationAdjustmentEntry>> generate(
      String csvFile) {
    final List<OutMigrationAdjustmentEntry> content = contentProvider.read(csvFile);
    final BinaryOperator<OutMigrationAdjustmentEntry> comparator =
        BinaryOperator.maxBy((a, b) -> 0);

    final Collector<
            OutMigrationAdjustmentEntry, ?, NavigableMap<LocalDate, OutMigrationAdjustmentEntry>>
        collector =
            Collectors.toMap(
                OutMigrationAdjustmentEntry::getEffectiveDate,
                Function.identity(),
                comparator,
                TreeMap::new);

    return LookupGenerator.generateCrossReferenceToMap(
        OutMigrationAdjustmentEntry::getCounty, content, HashMap::new, collector);
  }
}
