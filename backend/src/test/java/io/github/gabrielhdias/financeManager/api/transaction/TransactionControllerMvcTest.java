package io.github.gabrielhdias.financeManager.api.transaction;

import io.github.gabrielhdias.financeManager.config.security.SecurityConfig;
import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.transaction.Transaction;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionService;
import io.github.gabrielhdias.financeManager.domain.transaction.TransactionType;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
class TransactionControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoToken() throws Exception {
        mockMvc.perform(
                get("/transactions")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateTransactionWhenAuthenticated() throws Exception {
        Long userId = 1L;
        Long accountId = 10L;
        Long categoryId = 20L;

        Account account = mock(Account.class);
        Category category = mock(Category.class);
        Transaction transaction = mock(Transaction.class);

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(account.getId())
            .thenReturn(accountId);

        when(account.getName())
            .thenReturn("Nubank");

        when(category.getId())
            .thenReturn(categoryId);

        when(category.getName())
            .thenReturn("Alimentação");

        when(transaction.getId())
            .thenReturn(100L);

        when(transaction.getDescription())
            .thenReturn("Mercado");

        when(transaction.getAmount())
            .thenReturn(new BigDecimal("150.00"));

        when(transaction.getDate())
            .thenReturn(LocalDate.of(2026, 10, 1));

        when(transaction.getType())
            .thenReturn(TransactionType.EXPENSE);

        when(transaction.getAccount())
            .thenReturn(account);

        when(transaction.getCategory())
            .thenReturn(category);

        when(transactionService.create(
            userId,
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            accountId,
            categoryId
        )).thenReturn(transaction);

        mockMvc.perform(
                post("/transactions")
                    .with(jwt().jwt(jwt ->
                        jwt.subject(userId.toString())
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "description": "Mercado",
                                  "amount": 150.00,
                                  "date": "2026-10-01",
                                  "type": "EXPENSE",
                                  "accountId": 10,
                                  "categoryId": 20
                                }
                                """)
            )
            .andExpect(status().isCreated())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON
                )
            )
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.description")
                .value("Mercado"))
            .andExpect(jsonPath("$.amount")
                .value(150.00))
            .andExpect(jsonPath("$.date")
                .value("2026-10-01"))
            .andExpect(jsonPath("$.type")
                .value("EXPENSE"))
            .andExpect(jsonPath("$.accountId")
                .value(10))
            .andExpect(jsonPath("$.accountName")
                .value("Nubank"))
            .andExpect(jsonPath("$.categoryId")
                .value(20))
            .andExpect(jsonPath("$.categoryName")
                .value("Alimentação"));
    }

    @Test
    void shouldReturnBadRequestWhenTransactionDataIsInvalid() throws Exception {
        when(authenticatedUser.getId())
            .thenReturn(1L);

        mockMvc.perform(
                post("/transactions")
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                                {
                                  "description": "",
                                  "amount": -10.00,
                                  "date": null,
                                  "type": null,
                                  "accountId": null,
                                  "categoryId": null
                                }
                                """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status")
                .value(400))
            .andExpect(jsonPath("$.error")
                .value("Validation Error"))
            .andExpect(jsonPath("$.message")
                .value("Dados inválidos"))
            .andExpect(jsonPath("$.fields.description")
                .exists())
            .andExpect(jsonPath("$.fields.amount")
                .exists())
            .andExpect(jsonPath("$.fields.date")
                .exists())
            .andExpect(jsonPath("$.fields.type")
                .exists())
            .andExpect(jsonPath("$.fields.accountId")
                .exists())
            .andExpect(jsonPath("$.fields.categoryId")
                .exists());
    }
}
