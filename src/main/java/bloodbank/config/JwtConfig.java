package bloodbank.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Один секретный ключ и два объекта на его основе: кодировщик (подписывает токен при входе)
 * и декодер (проверяет подпись, срок и издателя на каждом запросе).
 * Сам секрет лежит в yaml (в проде это переменная CSS_JWT_SECRET), в Java его нет.
 */
@Configuration
public class JwtConfig {

    @Bean
    SecretKey jwtKey(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // короткий секрет вроде "secret" легко подобрать, поэтому приложение даже не запускаем
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret должен быть не короче 32 символов");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // обычные проверки (срок exp) плюс: iss должен быть наш. Токен чужого сервиса не подойдёт
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(TokenService.ISSUER);
        decoder.setJwtValidator(validator);
        return decoder;
    }
}
