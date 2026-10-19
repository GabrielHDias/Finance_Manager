package io.github.gabrielhdias.financeManager.domain.transaction.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TransactionCsvParserTest {
    private final TransactionCsvParser parser = new TransactionCsvParser();

    private byte[] csv(String content) { return content.getBytes(StandardCharsets.UTF_8); }

    @Test
    void readsBomQuotedFieldsBrazilianValuesAndPhysicalLineNumbers() {
        var rows = parser.parse(csv("\uFEFFdata;descricao;valor;tipo\r\n\r\n19/10/2026;\"Café; e \"\"pão\"\"\";12,50;DESPESA\r\n2026-10-20;\"Duas\nlinhas\";1000;RECEITA\r\n2026-10-21;Fim;1;INCOME"));
        assertEquals(3, rows.size());
        assertEquals(3, rows.getFirst().line());
        assertEquals("Café; e \"pão\"", rows.getFirst().description());
        assertEquals("12.50", rows.getFirst().amount());
        assertEquals("2026-10-19", rows.getFirst().date());
        assertEquals("EXPENSE", rows.getFirst().type());
        assertEquals("Duas\nlinhas", rows.get(1).description());
        assertEquals(6, rows.get(2).line());
        assertTrue(rows.stream().allMatch(row -> row.status() == CsvImportRow.Status.READY));
    }

    @Test
    void readsCommaSeparatedReorderedCaseInsensitiveHeaders() {
        var row = parser.parse(csv("TIPO,valor,descricao,data\nexpense,\"10,25\",Exemplo,2026-10-19")).getFirst();
        assertEquals(CsvImportRow.Status.READY, row.status());
        assertEquals("10.25", row.amount());
    }

    @Test
    void collectsAllErrorsWithoutDiscardingValidRows() {
        var rows = parser.parse(csv("data;descricao;valor;tipo\n31/02/2026;;-1;outro\n2026-10-19;Correta;2.30;INCOME\n2026-10-19;Coluna faltando;1\n2026-10-19;Extra;1;EXPENSE;extra"));
        assertEquals(4, rows.getFirst().errors().size());
        assertEquals(CsvImportRow.Status.READY, rows.get(1).status());
        assertEquals(CsvImportRow.Status.INVALID, rows.get(2).status());
        assertEquals(CsvImportRow.Status.INVALID, rows.get(3).status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1.234", "1.234,56", "NaN", "1e3", "-5", "123456789012345678"})
    void rejectsAmbiguousOrInvalidAmounts(String amount) {
        var row = parser.parse(csv("data;descricao;valor;tipo\n2026-10-19;Exemplo;" + amount + ";DESPESA")).getFirst();
        assertEquals(CsvImportRow.Status.INVALID, row.status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "data;descricao;valor;tipo\n", "data;descricao;valor;valor\n", "data;descricao;valor;tipo;extra\n", "data;descricao;valor;tipo\n2026-10-19;\"sem fechamento;1;DESPESA"})
    void rejectsEmptyMalformedOrIncompatibleFiles(String content) {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(csv(content)));
    }

    @Test
    void rejectsInvalidEncodingAndOversizedFiles() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new byte[] {(byte) 0xc3, 0x28}));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new byte[TransactionCsvParser.MAX_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(csv("data;descricao;valor;tipo\n2026-10-19;zero\0byte;1;INCOME")));
    }

    @Test
    void acceptsOneThousandRowsButRejectsAnExtraRow() {
        String content = "data;descricao;valor;tipo\n" + "2026-10-19;Exemplo;1;DESPESA\n".repeat(1000);
        assertEquals(1000, parser.parse(csv(content)).size());
        assertThrows(IllegalArgumentException.class, () -> parser.parse(csv(content + "2026-10-19;Extra;1;DESPESA")));
    }
}
