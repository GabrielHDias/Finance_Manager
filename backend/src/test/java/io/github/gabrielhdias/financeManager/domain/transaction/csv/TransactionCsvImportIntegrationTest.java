package io.github.gabrielhdias.financeManager.domain.transaction.csv;

import io.github.gabrielhdias.financeManager.domain.account.*;
import io.github.gabrielhdias.financeManager.domain.category.*;
import io.github.gabrielhdias.financeManager.domain.user.*;
import io.github.gabrielhdias.financeManager.domain.transaction.*;
import io.github.gabrielhdias.financeManager.domain.exception.*;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionCsvImportIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired TransactionCsvImportService service;
    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired TransactionRepository transactions;
    @Autowired MockMvc mvc;

    private static final byte[] FILE = bytes("data;descricao;valor;tipo\n2026-10-01;Salário;1000;RECEITA\n02/10/2026;Mercado;100,50;DESPESA\n2026-10-02;Mercado;100.50;EXPENSE");
    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    private Fixture fixture() {
        User user = users.save(new User("CSV", UUID.randomUUID() + "@example.test", "test-hash"));
        return new Fixture(user.getId(), accounts.save(new Account("Conta CSV", user)).getId(),
            categories.save(new Category("Categoria CSV", user)).getId());
    }

    private CsvImportPreview preview(Fixture f, byte[] file) { return service.preview(f.user(), f.account(), f.category(), file); }
    private CsvImportResult confirm(Fixture f, byte[] file, String hash) { return service.confirm(f.user(), f.account(), f.category(), file, hash); }

    @Test
    void previewDoesNotWriteAndRepeatedConfirmationsAreIdempotent() {
        Fixture f = fixture();
        var preview = preview(f, FILE);
        assertEquals(2, preview.ready());
        assertEquals(1, preview.duplicates());
        assertFalse(transactions.existsByAccountId(f.account()));
        assertEquals(new CsvImportResult(2, 1), confirm(f, FILE, preview.hash()));
        assertEquals(new CsvImportResult(0, 3), confirm(f, FILE, preview.hash()));
        assertEquals(3, preview(f, FILE).duplicates());
        assertEquals(new BigDecimal("899.50"), transactions.calculateBalanceByAccountId(f.account(), TransactionType.INCOME));
    }

    @Test
    void matchesManuallyCreatedTransactionsAndRechecksAfterPreview() {
        Fixture f = fixture();
        var preview = preview(f, FILE);
        transactions.save(new Transaction("Mercado", new BigDecimal("100.50"), LocalDate.of(2026, 10, 2),
            TransactionType.EXPENSE, accounts.findById(f.account()).orElseThrow(), categories.findById(f.category()).orElseThrow()));
        assertEquals(new CsvImportResult(1, 2), confirm(f, FILE, preview.hash()));
        assertEquals(2, transactions.findAllByFilters(f.user(), null, null, null, f.account(), f.category()).size());
    }

    @Test
    void invalidRowsOrChangedFileCannotProducePartialImports() {
        Fixture f = fixture();
        byte[] invalid = bytes(new String(FILE, StandardCharsets.UTF_8) + "\n2026-02-31;Inválida;0;DESPESA");
        var preview = preview(f, invalid);
        assertEquals(1, preview.invalid());
        assertThrows(BusinessRuleException.class, () -> confirm(f, invalid, preview.hash()));
        assertThrows(BusinessRuleException.class, () -> confirm(f, FILE, preview.hash()));
        assertFalse(transactions.existsByAccountId(f.account()));
    }

    @Test
    void resourcesAndHashesAreScopedToTheirOwnerAndSelection() {
        Fixture owner = fixture(), other = fixture();
        var preview = preview(owner, FILE);
        assertThrows(ResourceNotFoundException.class, () -> service.preview(other.user(), owner.account(), other.category(), FILE));
        assertThrows(ResourceNotFoundException.class, () -> service.preview(other.user(), other.account(), owner.category(), FILE));
        assertThrows(ResourceNotFoundException.class, () -> service.confirm(other.user(), owner.account(), owner.category(), FILE, preview.hash()));
        assertThrows(ResourceNotFoundException.class, () -> service.confirm(other.user(), other.account(), owner.category(), FILE, preview.hash()));
        assertThrows(BusinessRuleException.class, () -> confirm(other, FILE, preview.hash()));
        assertFalse(transactions.existsByAccountId(owner.account()));
        assertFalse(transactions.existsByAccountId(other.account()));
    }

    @Test
    void concurrentConfirmationsDoNotDuplicateRows() throws Exception {
        Fixture f = fixture();
        String hash = preview(f, FILE).hash();
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return confirm(f, FILE, hash); });
            var second = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return confirm(f, FILE, hash); });
            int imported = first.get(20, TimeUnit.SECONDS).imported() + second.get(20, TimeUnit.SECONDS).imported();
            assertEquals(2, imported);
            assertEquals(2, transactions.findAllByFilters(f.user(), null, null, null, f.account(), f.category()).size());
        }
    }

    @Test
    void acceptsMultipartAndRequiresAuthenticationAndRequiredParts() throws Exception {
        Fixture f = fixture();
        mvc.perform(multipart("/transactions/import/preview").file(new MockMultipartFile("file", "test.csv", "text/csv", FILE))
                .param("accountId", f.account().toString()).param("categoryId", f.category().toString()))
            .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/transactions/import/preview").file(new MockMultipartFile("file", "test.csv", "text/csv", FILE))
                .param("accountId", f.account().toString()).param("categoryId", f.category().toString())
                .with(jwt().jwt(jwt -> jwt.subject(f.user().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.ready").value(2)).andExpect(jsonPath("$.duplicates").value(1));
        mvc.perform(multipart("/transactions/import/confirm")
                .with(jwt().jwt(jwt -> jwt.subject(f.user().toString()))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        assertFalse(transactions.existsByAccountId(f.account()));
    }

    private record Fixture(Long user, Long account, Long category) { }
}
