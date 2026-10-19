package io.github.gabrielhdias.financeManager.domain.transaction.csv;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.account.AccountRepository;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.category.CategoryRepository;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
import io.github.gabrielhdias.financeManager.domain.exception.ResourceNotFoundException;
import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionRepository;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
public class TransactionCsvImportService {
    private final TransactionCsvParser parser;
    private final AccountRepository accounts;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;

    public TransactionCsvImportService(TransactionCsvParser parser, AccountRepository accounts,
        CategoryRepository categories, TransactionRepository transactions) {
        this.parser = parser;
        this.accounts = accounts;
        this.categories = categories;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public CsvImportPreview preview(Long userId, Long accountId, Long categoryId, byte[] file) {
        Account account = accounts.findByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        Category category = findCategory(userId, categoryId);
        return prepare(userId, account, category, file);
    }

    @Transactional
    public CsvImportResult confirm(Long userId, Long accountId, Long categoryId, byte[] file, String previewHash) {
        // Serialize imports for this account so simultaneous confirmations cannot insert the same rows.
        Account account = accounts.findForImport(accountId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        Category category = findCategory(userId, categoryId);
        CsvImportPreview preview = prepare(userId, account, category, file);
        if (!preview.hash().equals(previewHash)) {
            throw new BusinessRuleException("O arquivo ou a seleção mudou. Gere uma nova prévia antes de importar.");
        }
        if (preview.invalid() > 0) {
            throw new BusinessRuleException("Corrija todas as linhas inválidas e gere uma nova prévia. Nenhuma transação foi importada.");
        }
        List<Transaction> pending = preview.rows().stream()
            .filter(row -> row.status() == CsvImportRow.Status.READY)
            .map(row -> new Transaction(row.description(), new BigDecimal(row.amount()), LocalDate.parse(row.date()),
                TransactionType.valueOf(row.type()), account, category)).toList();
        transactions.saveAll(pending);
        transactions.flush();
        return new CsvImportResult(pending.size(), preview.duplicates());
    }

    private Category findCategory(Long userId, Long categoryId) {
        return categories.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
    }

    private CsvImportPreview prepare(Long userId, Account account, Category category, byte[] file) {
        List<CsvImportRow> parsed = parser.parse(file);
        List<CsvImportRow> valid = parsed.stream().filter(row -> row.status() == CsvImportRow.Status.READY).toList();
        Set<Key> existing = new HashSet<>();
        if (!valid.isEmpty()) {
            LocalDate start = valid.stream().map(row -> LocalDate.parse(row.date())).min(LocalDate::compareTo).orElseThrow();
            LocalDate end = valid.stream().map(row -> LocalDate.parse(row.date())).max(LocalDate::compareTo).orElseThrow();
            for (Transaction transaction : transactions.findAllByFilters(userId, start, end, null, account.getId(), category.getId())) {
                existing.add(new Key(transaction.getDate(), transaction.getDescription().trim(),
                    transaction.getAmount().setScale(2).toPlainString(), transaction.getType().name()));
            }
        }
        Set<Key> inFile = new HashSet<>();
        List<CsvImportRow> rows = new ArrayList<>();
        int ready = 0, duplicates = 0, invalid = 0;
        for (CsvImportRow row : parsed) {
            if (row.status() == CsvImportRow.Status.INVALID) {
                rows.add(row);
                invalid++;
            } else {
                Key key = new Key(LocalDate.parse(row.date()), row.description(), row.amount(), row.type());
                if (existing.contains(key)) {
                    rows.add(row.duplicate("Já existe uma transação idêntica nesta conta e categoria."));
                    duplicates++;
                } else if (!inFile.add(key)) {
                    rows.add(row.duplicate("Registro repetido no arquivo."));
                    duplicates++;
                } else {
                    rows.add(row);
                    ready++;
                }
            }
        }
        return new CsvImportPreview(hash(userId, account.getId(), category.getId(), file), account.getName(),
            category.getName(), ready, duplicates, invalid, List.copyOf(rows));
    }

    private String hash(Long userId, Long accountId, Long categoryId, byte[] file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((userId + ":" + accountId + ":" + categoryId + ":").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest(file));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private record Key(LocalDate date, String description, String amount, String type) { }
}
