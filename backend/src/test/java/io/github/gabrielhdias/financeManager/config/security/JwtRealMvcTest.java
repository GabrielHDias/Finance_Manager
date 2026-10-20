package io.github.gabrielhdias.financeManager.config.security;
import io.github.gabrielhdias.financeManager.api.account.AccountController;
import io.github.gabrielhdias.financeManager.domain.account.AccountService;
import io.github.gabrielhdias.financeManager.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
@Import({SecurityTestConfig.class, JwtConfig.class, AuthenticatedUser.class})
@TestPropertySource(properties = "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
class JwtRealMvcTest {
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @MockitoBean AccountService service;
    private String token(JwtEncoder source, String subject, Instant expiry, MacAlgorithm algorithm) {
        return source.encode(JwtEncoderParameters.from(JwsHeader.with(algorithm).build(), JwtClaimsSet.builder()
            .subject(subject).issuedAt(expiry.minusSeconds(3600)).expiresAt(expiry).build())).getTokenValue();
    }
    @Test void validTokenPassesRealFilterAndDecoder() throws Exception {
        mvc.perform(get("/accounts").header("Authorization", "Bearer " + token(encoder, "1", Instant.now().plusSeconds(3600), MacAlgorithm.HS256)))
            .andExpect(status().isOk());
    }
    @Test void invalidSubjectsAndExpirationReturnStandard401() throws Exception {
        for (String subject : new String[]{"abc", "-1", "0", "9223372036854775808", ""}) {
            mvc.perform(get("/accounts").header("Authorization", "Bearer " + token(encoder, subject, Instant.now().plusSeconds(3600), MacAlgorithm.HS256)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        }
        mvc.perform(get("/accounts").header("Authorization", "Bearer " + token(encoder, "1", Instant.now().minusSeconds(120), MacAlgorithm.HS256)))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.fields").isMap());
    }
    @Test void missingMandatoryClaimsAreRejected() throws Exception {
        for (JwtClaimsSet claims : java.util.List.of(
            JwtClaimsSet.builder().subject("1").issuedAt(Instant.now()).build(),
            JwtClaimsSet.builder().issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build())) {
            String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
            mvc.perform(get("/accounts").header("Authorization", "Bearer " + value))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        }
    }
    @Test void wrongSignatureAndAlgorithmAreRejected() throws Exception {
        JwtEncoder wrong = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(new byte[32], "HmacSHA256")).algorithm(MacAlgorithm.HS256).build();
        JwtEncoder other = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(new byte[48], "HmacSHA384")).algorithm(MacAlgorithm.HS384).build();
        for (String value : new String[]{token(wrong,"1",Instant.now().plusSeconds(1000),MacAlgorithm.HS256),
            token(other,"1",Instant.now().plusSeconds(1000),MacAlgorithm.HS384)}) {
            mvc.perform(get("/accounts").header("Authorization", "Bearer " + value)).andExpect(status().isUnauthorized());
        }
    }
}
