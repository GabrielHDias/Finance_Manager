package io.github.gabrielhdias.financeManager.api.transaction;

import io.github.gabrielhdias.financeManager.config.security.SecurityConfig;
import io.github.gabrielhdias.financeManager.config.security.SecurityTestConfig;
import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.domain.category.Category;
import io.github.gabrielhdias.financeManager.domain.exception.BusinessRuleException;
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
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(SecurityTestConfig.class)
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
    void shouldRejectAmountsOutsideTheSupportedRangeSpecifically() throws Exception {
        for (String amount : new String[]{"100000000000000000.00", "1.001", "0.00", "-0.01"}) {
            String body = "{\"description\":\"Range\",\"amount\":\""+amount+"\",\"date\":\"2026-10-20\",\"type\":\"INCOME\",\"accountId\":1,\"categoryId\":1}";
            mockMvc.perform(post("/transactions").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.amount").exists());
        }
    }

    @Test
    void shouldRejectMalformedBodiesParametersAndAmountsWithConsistentJson() throws Exception {
        for (String body : new String[]{"{", "{\"type\":\"UNKNOWN\"}", "{\"date\":\"2026-02-31\"}",
            "{\"amount\":\"100000000000000000.00\"}", "{\"amount\":\"1.001\"}"}) {
            mockMvc.perform(post("/transactions").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields").isMap()).andExpect(jsonPath("$.timestamp").isString());
        }
        for (String query : new String[]{"type=UNKNOWN", "startDate=invalid", "accountId=abc", "page=bad"}) {
            mockMvc.perform(get("/transactions/page?" + query).with(jwt()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
    }

    @Test
    void shouldReturnUnauthorizedWhenRequestHasNoToken()
        throws Exception {

        mockMvc.perform(
                get("/transactions")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateTransactionWhenAuthenticated()
        throws Exception {

        Long userId = 1L;

        Transaction transaction = createMockTransaction();

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(transactionService.create(
            userId,
            "Mercado",
            new BigDecimal("150.00"),
            LocalDate.of(2026, 10, 1),
            TransactionType.EXPENSE,
            10L,
            20L
        )).thenReturn(transaction);

        mockMvc.perform(
                post("/transactions")
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
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
            .andExpect(jsonPath("$.id")
                .value(100))
            .andExpect(jsonPath("$.description")
                .value("Mercado"))
            .andExpect(jsonPath("$.amount")
                .value(150.00))
            .andExpect(jsonPath("$.type")
                .value("EXPENSE"));
    }

    @Test
    void shouldListTransactionsWithoutFilters()
        throws Exception {

        Long userId = 1L;

        Transaction transaction = createMockTransaction();

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(transactionService.filterByUser(
            userId,
            null,
            null,
            null,
            null,
            null
        )).thenReturn(
            List.of(transaction)
        );

        mockMvc.perform(
                get("/transactions")
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id")
                .value(100))
            .andExpect(jsonPath("$[0].description")
                .value("Mercado"));
    }

    @Test
    void shouldListTransactionsWithFilters()
        throws Exception {

        Long userId = 1L;

        Transaction transaction = createMockTransaction();

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(transactionService.filterByUser(
            userId,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31),
            TransactionType.EXPENSE,
            10L,
            20L
        )).thenReturn(
            List.of(transaction)
        );

        mockMvc.perform(
                get("/transactions")
                    .param(
                        "startDate",
                        "2026-10-01"
                    )
                    .param(
                        "endDate",
                        "2026-10-31"
                    )
                    .param(
                        "type",
                        "EXPENSE"
                    )
                    .param(
                        "accountId",
                        "10"
                    )
                    .param(
                        "categoryId",
                        "20"
                    )
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].description")
                .value("Mercado"));
    }

    @Test
    void shouldReturnConflictWhenPeriodIsInvalid()
        throws Exception {

        Long userId = 1L;

        when(authenticatedUser.getId())
            .thenReturn(userId);

        when(transactionService.filterByUser(
            userId,
            LocalDate.of(2026, 10, 31),
            LocalDate.of(2026, 10, 1),
            null,
            null,
            null
        )).thenThrow(
            new BusinessRuleException(
                "A data inicial não pode ser posterior à data final"
            )
        );

        mockMvc.perform(
                get("/transactions")
                    .param(
                        "startDate",
                        "2026-10-31"
                    )
                    .param(
                        "endDate",
                        "2026-10-01"
                    )
                    .with(jwt().jwt(jwt ->
                        jwt.subject("1")
                    ))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status")
                .value(409))
            .andExpect(jsonPath("$.message")
                .value(
                    "A data inicial não pode ser posterior à data final"
                ));
    }

    @Test
    void shouldReturnBadRequestWhenTransactionDataIsInvalid()
        throws Exception {

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
            .andExpect(jsonPath("$.fields.description")
                .exists())
            .andExpect(jsonPath("$.fields.amount")
                .exists());
    }

    @Test
    void shouldReturnPageWithStableMetadataAndFilters() throws Exception {
        Transaction transaction = createMockTransaction();
        when(authenticatedUser.getId()).thenReturn(1L);
        when(transactionService.pageByUser(
            1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
            TransactionType.EXPENSE, 10L, 20L, 1, 1
        )).thenReturn(new org.springframework.data.domain.PageImpl<>(
            List.of(transaction), org.springframework.data.domain.PageRequest.of(1, 1), 2
        ));
        mockMvc.perform(get("/transactions/page").with(jwt())
                .param("startDate", "2026-10-01").param("endDate", "2026-10-31")
                .param("type", "EXPENSE").param("accountId", "10").param("categoryId", "20")
                .param("page", "1").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(100))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldProtectPaginatedEndpoint() throws Exception {
        mockMvc.perform(get("/transactions/page")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnBadRequestForInvalidPageSize() throws Exception {
        when(authenticatedUser.getId()).thenReturn(1L);
        when(transactionService.pageByUser(1L, null, null, null, null, null, 0, 101))
            .thenThrow(new IllegalArgumentException("Tamanho inválido"));
        mockMvc.perform(get("/transactions/page").with(jwt()).param("size", "101"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/transactions/page").with(jwt()).param("page", "abc"))
            .andExpect(status().isBadRequest());
    }

    private Transaction createMockTransaction() {
        Account account = mock(Account.class);
        Category category = mock(Category.class);
        Transaction transaction = mock(Transaction.class);

        when(account.getId())
            .thenReturn(10L);

        when(account.getName())
            .thenReturn("Nubank");

        when(category.getId())
            .thenReturn(20L);

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

        return transaction;
    }
}
