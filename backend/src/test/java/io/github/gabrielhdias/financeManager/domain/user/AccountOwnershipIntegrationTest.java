package io.github.gabrielhdias.financeManager.domain.user;

import io.github.gabrielhdias.financeManager.domain.account.Account;
import io.github.gabrielhdias.financeManager.integration.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountOwnershipIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired io.github.gabrielhdias.financeManager.domain.account.AccountRepository accounts;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void emailChangeRequiresPasswordAndDeletionRemovesOnlyThatUser() throws Exception {
        User owner = users.saveAndFlush(new User("Dono", "owner@example.test", passwordEncoder.encode("senha-atual")));
        User other = users.saveAndFlush(new User("Outro", "other@example.test", passwordEncoder.encode("outra-senha")));
        accounts.saveAndFlush(new Account("Conta do dono", owner));
        accounts.saveAndFlush(new Account("Conta do outro", other));
        String subject = owner.getId().toString();

        mockMvc.perform(put("/users/me/email").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"email\":\"novo@example.test\",\"currentPassword\":\"errada\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/users/me/email").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"email\":\"other@example.test\",\"currentPassword\":\"senha-atual\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(put("/users/me/email").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json")
                .content("{\"email\":\"novo@example.test\",\"currentPassword\":\"senha-atual\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("novo@example.test"));

        mockMvc.perform(delete("/users/me").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json").content("{\"currentPassword\":\"senha-atual\"}"))
            .andExpect(status().isNoContent());
        assertFalse(users.existsById(owner.getId()));
        assertTrue(users.existsById(other.getId()));
        assertTrue(accounts.findAllByUserId(other.getId()).size() == 1);
        mockMvc.perform(get("/users/me").with(jwt().jwt(token -> token.subject(subject))))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/accounts").with(jwt().jwt(token -> token.subject(subject)))
                .contentType("application/json").content("{\"name\":\"Depois\"}"))
            .andExpect(status().isNotFound());
    }
}
