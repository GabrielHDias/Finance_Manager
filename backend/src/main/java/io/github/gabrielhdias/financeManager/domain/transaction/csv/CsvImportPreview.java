package io.github.gabrielhdias.financeManager.domain.transaction.csv;

import java.util.List;

public record CsvImportPreview(
    String hash, String accountName, String categoryName,
    int ready, int duplicates, int invalid, List<CsvImportRow> rows
) { }
