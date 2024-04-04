package gov.cms.fiss.pricers.opps.core.tables;

import gov.cms.fiss.pricers.common.csv.CsvContentReader;
import gov.cms.fiss.pricers.common.csv.CsvIngestionConfiguration;
import gov.cms.fiss.pricers.common.csv.LookupGenerator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ApcRateHistoryLookupGenerator {
  private static final String CSV_FILE_PATTERN = "/apc-rate-%s.csv";

  private final CsvContentReader<ApcRateHistoryEntry> contentProvider =
      new CsvContentReader<>(ApcRateHistoryEntry.class)
          .customizeSchema(CsvContentReader.HEADER_ROW_CUSTOMIZER);

  public ApcRateHistoryLookupGenerator(CsvIngestionConfiguration csvIngestionConfiguration) {
    if (!csvIngestionConfiguration.isValidationEnabled()) {
      contentProvider.disableValidation();
    }
  }

  /**
   * Creates a lookup table for APC Rate data for the given pricer year.
   *
   * @return the populated table
   */
  public Map<String, NavigableMap<LocalDate, ApcRateHistoryEntry>> generate(int pricerYear) {
    return generate(String.format(CSV_FILE_PATTERN, pricerYear));
  }

  /**
   * Creates a lookup table for APC Rate History data with the given CSV file name.
   *
   * @return the populated table
   */
  public Map<String, NavigableMap<LocalDate, ApcRateHistoryEntry>> generate(String csvFile) {
    // Read in CSV data and sort by percent
    final List<ApcRateHistoryEntry> content =
        contentProvider.read(csvFile).stream()
            .sorted(Comparator.comparing(ApcRateHistoryEntry::getReimbursementRanking))
            .collect(Collectors.toList());

    // Set rank based on current ordering
    int rank = 0;
    final List<ApcRateHistoryEntry> rankedContent = new ArrayList<>();
    for (final ApcRateHistoryEntry entry : content) {
      // Due to the builder pattern, a new list will need made with updated entries
      final ApcRateHistoryEntry copy = entry.copyBuilder().rank(rank).build();
      rankedContent.add(copy);
      rank++;
    }

    final BinaryOperator<ApcRateHistoryEntry> comparator = BinaryOperator.maxBy((a, b) -> 0);
    final Collector<ApcRateHistoryEntry, ?, NavigableMap<LocalDate, ApcRateHistoryEntry>>
        collector =
            Collectors.toMap(
                ApcRateHistoryEntry::getEffectiveDate,
                Function.identity(),
                comparator,
                TreeMap::new);

    return LookupGenerator.generateCrossReferenceToMap(
        ApcRateHistoryEntry::getApc, rankedContent, HashMap::new, collector);
  }
}
