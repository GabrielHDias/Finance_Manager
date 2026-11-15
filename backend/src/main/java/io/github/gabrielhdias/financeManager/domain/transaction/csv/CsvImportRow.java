package io.github.gabrielhdias.financeManager.domain.transaction.csv;

import java.util.List;

public record CsvImportRow(
    int line, String date, String description, String amount, String type,
    Status status, List<String> errors, List<String> tagNames
) {
    public enum Status { READY, DUPLICATE, INVALID }

    public CsvImportRow(int line, String date, String description, String amount, String type, Status status, List<String> errors) {
        this(line, date, description, amount, type, status, errors, List.of());
    }

    public CsvImportRow duplicate(String reason) {
        return new CsvImportRow(line, date, description, amount, type, Status.DUPLICATE, List.of(reason), tagNames);
    }

    public CsvImportRow invalid(String reason) {
        return new CsvImportRow(line, date, description, amount, type, Status.INVALID, List.of(reason), tagNames);
    }
}
