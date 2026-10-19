package io.github.gabrielhdias.financeManager.api.transaction;

import io.github.gabrielhdias.financeManager.domain.transaction.csv.CsvImportPreview;
import io.github.gabrielhdias.financeManager.domain.transaction.csv.CsvImportResult;
import io.github.gabrielhdias.financeManager.domain.transaction.csv.TransactionCsvImportService;
import io.github.gabrielhdias.financeManager.domain.transaction.csv.TransactionCsvParser;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/transactions/import")
@Tag(name = "Transaction import", description = "Prévia e confirmação de importação CSV")
@SecurityRequirement(name = "bearerAuth")
public class TransactionCsvImportController {
    private final TransactionCsvImportService service;
    private final AuthenticatedUser authenticatedUser;

    public TransactionCsvImportController(TransactionCsvImportService service, AuthenticatedUser authenticatedUser) {
        this.service = service;
        this.authenticatedUser = authenticatedUser;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvImportPreview preview(@RequestParam MultipartFile file, @RequestParam Long accountId,
        @RequestParam Long categoryId) {
        return service.preview(authenticatedUser.getId(), accountId, categoryId, read(file));
    }

    @PostMapping(value = "/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvImportResult confirm(@RequestParam MultipartFile file, @RequestParam Long accountId,
        @RequestParam Long categoryId, @RequestParam String previewHash) {
        return service.confirm(authenticatedUser.getId(), accountId, categoryId, read(file), previewHash);
    }

    private byte[] read(MultipartFile file) {
        try (var input = file.getInputStream()) {
            return input.readNBytes(TransactionCsvParser.MAX_BYTES + 1);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo CSV. Selecione-o novamente.");
        }
    }
}
