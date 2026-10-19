package io.github.gabrielhdias.financeManager.domain.transaction.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class TransactionCsvParser {
    public static final int MAX_BYTES = 1024 * 1024;
    public static final int MAX_ROWS = 1000;
    private static final Set<String> HEADERS = Set.of("data", "descricao", "valor", "tipo");
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter
        .ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public List<CsvImportRow> parse(byte[] bytes) {
        if (bytes.length == 0) throw new IllegalArgumentException("O arquivo CSV está vazio.");
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("O arquivo deve ter no máximo 1 MiB.");
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Salve o arquivo CSV em UTF-8.");
        }
        if (content.startsWith("\uFEFF")) content = content.substring(1);
        if (content.indexOf('\0') >= 0) throw new IllegalArgumentException("O arquivo contém caracteres inválidos.");
        String headerLine = content.lines().filter(line -> !line.isBlank()).findFirst().orElse("");
        char delimiter = headerLine.contains(";") ? ';' : ',';
        CSVFormat format = CSVFormat.RFC4180.builder()
            .setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true).setIgnoreSurroundingSpaces(true).setTrim(true)
            .setAllowMissingColumnNames(true).setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL).get();
        try (CSVParser parser = CSVParser.parse(content, format)) {
            List<String> headers = parser.getHeaderNames().stream()
                .map(name -> name.toLowerCase(Locale.ROOT)).toList();
            if (headers.size() != 4 || !new HashSet<>(headers).equals(HEADERS)) {
                throw new IllegalArgumentException("Use os cabeçalhos data, descricao, valor e tipo, sem colunas extras.");
            }
            List<CsvImportRow> rows = new ArrayList<>();
            int cursor = 0;
            int physicalLine = 1;
            for (CSVRecord record : parser) {
                if (rows.size() == MAX_ROWS) throw new IllegalArgumentException("Importe no máximo 1000 registros por arquivo.");
                int start = Math.toIntExact(record.getCharacterPosition());
                while (start < content.length() && (content.charAt(start) == '\r' || content.charAt(start) == '\n')) start++;
                while (cursor < start) {
                    char c = content.charAt(cursor++);
                    if (c == '\n' || c == '\r' && (cursor == content.length() || content.charAt(cursor) != '\n')) physicalLine++;
                }
                rows.add(parseRow(record, headers, physicalLine));
            }
            if (rows.isEmpty()) throw new IllegalArgumentException("O arquivo não contém transações.");
            return List.copyOf(rows);
        } catch (IOException | UncheckedIOException exception) {
            throw new IllegalArgumentException("CSV malformado. Confira as aspas e os separadores.");
        }
    }

    private CsvImportRow parseRow(CSVRecord record, List<String> headers, int line) {
        List<String> errors = new ArrayList<>();
        if (record.size() != 4) errors.add("A linha deve conter exatamente quatro colunas.");
        String date = cell(record, headers.indexOf("data"));
        String description = cell(record, headers.indexOf("descricao"));
        String amount = cell(record, headers.indexOf("valor"));
        String type = cell(record, headers.indexOf("tipo"));
        if (description.isBlank() || description.length() > 255) errors.add("A descrição deve possuir de 1 a 255 caracteres.");
        try {
            LocalDate parsed;
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) parsed = LocalDate.parse(date);
            else if (date.matches("\\d{2}/\\d{2}/\\d{4}")) parsed = LocalDate.parse(date, BRAZILIAN_DATE);
            else throw new DateTimeParseException("format", date, 0);
            if (parsed.getYear() < 1) throw new DateTimeParseException("year", date, 0);
            date = parsed.toString();
        } catch (DateTimeParseException exception) {
            errors.add("Informe uma data válida em AAAA-MM-DD ou DD/MM/AAAA.");
        }
        if (!amount.matches("\\d{1,17}([.,]\\d{1,2})?") || new BigDecimal(amount.replace(',', '.')).signum() <= 0) {
            errors.add("O valor deve ser positivo, com até 17 dígitos inteiros e duas casas decimais, sem separador de milhar.");
        } else {
            amount = new BigDecimal(amount.replace(',', '.')).setScale(2).toPlainString();
        }
        type = switch (type.toUpperCase(Locale.ROOT)) {
            case "RECEITA", "INCOME" -> "INCOME";
            case "DESPESA", "EXPENSE" -> "EXPENSE";
            default -> { errors.add("O tipo deve ser RECEITA ou DESPESA."); yield type; }
        };
        return new CsvImportRow(line, date, description, amount, type,
            errors.isEmpty() ? CsvImportRow.Status.READY : CsvImportRow.Status.INVALID, List.copyOf(errors));
    }

    private String cell(CSVRecord record, int index) {
        return index < record.size() ? record.get(index).trim() : "";
    }
}
