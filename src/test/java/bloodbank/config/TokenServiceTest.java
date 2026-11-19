package bloodbank.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Без Spring и без веба: TokenService выпускает токен, декодер с тем же секретом читает его обратно. */
class TokenServiceTest {

    private static final String SECRET = "test-secret-at-least-32-bytes-long!!";

    private final SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final TokenService tokens = new TokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)));
    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();

    private String tokenOf(String name, String role) {
        return tokens.issue(UsernamePasswordAuthenticationToken.authenticated(
                name, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void issuedTokenCarriesUserRoleIssuerAndExpiry() {
        Jwt jwt = decoder.decode(tokenOf("chief", "CHIEF_PHYSICIAN"));

        assertEquals("chief", jwt.getSubject());
        // в токене роль без приставки ROLE_
        assertEquals(List.of("CHIEF_PHYSICIAN"), jwt.getClaimAsStringList("roles"));
        // iss читаем как обычную строку: getIssuer() ждёт адрес сайта, а у нас просто имя продукта
        assertEquals("bloodbank", jwt.getClaimAsString("iss"));
        assertTrue(jwt.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void tokenLivesFifteenMinutes() {
        Jwt jwt = decoder.decode(tokenOf("technician", "LAB_TECHNICIAN"));

        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    void tokenHasNoPasswordInIt() {
        String token = tokenOf("technician", "LAB_TECHNICIAN");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertFalse(payload.contains("password"));
        assertFalse(payload.contains("tech-dev"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRefused() {
        SecretKey other = new SecretKeySpec("another-secret-at-least-32-bytes!!!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtDecoder strangerDecoder = NimbusJwtDecoder.withSecretKey(other).build();

        assertThrows(Exception.class, () -> strangerDecoder.decode(tokenOf("chief", "CHIEF_PHYSICIAN")));
    }
}
