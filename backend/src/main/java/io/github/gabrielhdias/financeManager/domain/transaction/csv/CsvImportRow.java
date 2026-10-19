package io.github.gabrielhdias.financeManager.domain.transaction.csv;

import java.util.List;

public record CsvImportRow(
    int line, String date, String description, String amount, String type,
    Status status, List<String> errors
) {
    public enum Status { READY, DUPLICATE, INVALID }

    public CsvImportRow duplicate(String reason) {
        return new CsvImportRow(line, date, description, amount, type, Status.DUPLICATE, List.of(reason));
    }
}
