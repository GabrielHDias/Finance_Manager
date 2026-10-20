package io.github.gabrielhdias.financeManager.api.error;
import io.github.gabrielhdias.financeManager.domain.user.*;
import io.github.gabrielhdias.financeManager.domain.account.*;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
class UniquenessRaceIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoSpyBean UserRepository users;
    @MockitoSpyBean AccountRepository accounts;
    @Test void simultaneousRegistrationsReturn201And409() throws Exception {
        String email = "race-register@example.test";
        CyclicBarrier barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            boolean exists = users.findByEmail(email).isPresent();
            barrier.await(10, TimeUnit.SECONDS); return exists;
        }).when(users).existsByEmail(email);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> register = () -> mvc.perform(post("/users").contentType("application/json")
                .content("{\"name\":\"Race\",\"email\":\"" + email + "\",\"password\":\"Disposable-Test-2026!\"}"))
                .andReturn().getResponse().getStatus();
            var first = pool.submit(register); var second = pool.submit(register);
            assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201,409);
        } finally { reset(users); }
    }
    @Test void simultaneousRenamesReturn200And409() throws Exception {
        User owner = users.saveAndFlush(new User("Race", "race-rename@example.test", "fixture-hash"));
        Account first = accounts.saveAndFlush(new Account("First",owner));
        Account second = accounts.saveAndFlush(new Account("Second",owner));
        CyclicBarrier barrier = new CyclicBarrier(2);
        doAnswer(invocation -> {
            boolean exists = accounts.findAllByUserId(owner.getId()).stream().anyMatch(a -> a.getName().equals("Shared"));
            barrier.await(10, TimeUnit.SECONDS); return exists;
        }).when(accounts).existsByUserIdAndName(owner.getId(),"Shared");
        try (var pool = Executors.newFixedThreadPool(2)) {
            java.util.function.Function<Long, Callable<Integer>> rename = id -> () -> mvc.perform(put("/accounts/" + id)
                .with(jwt().jwt(token -> token.subject(owner.getId().toString())))
                .contentType("application/json").content("{\"name\":\"Shared\"}"))
                .andReturn().getResponse().getStatus();
            var a = pool.submit(rename.apply(first.getId())); var b = pool.submit(rename.apply(second.getId()));
            assertThat(java.util.List.of(a.get(20,TimeUnit.SECONDS), b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        } finally { reset(accounts); }
    }
}
