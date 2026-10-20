package io.github.gabrielhdias.financeManager.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(
        @Value("${security.jwt.secret}") String secret
    ) {
        byte[] decodedSecret = Base64.getDecoder().decode(secret);

        return new SecretKeySpec(
            decodedSecret,
            "HmacSHA256"
        );
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder
            .withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS256)
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey secretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
            org.springframework.security.oauth2.jwt.JwtValidators.createDefault(), jwt -> {
                try {
                    String subject = jwt.getSubject();
                    if (jwt.getExpiresAt() == null || jwt.getIssuedAt() == null || subject == null || !subject.matches("[1-9][0-9]{0,18}") || Long.parseLong(subject) <= 0)
                        throw new IllegalArgumentException();
                    return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
                } catch (RuntimeException invalid) {
                    return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Invalid subject", null));
                }
            }));
        return decoder;
    }
}
