package bloodbank.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Пишет и подписывает пропуск (JWT). В токен кладём только четыре факта:
 * кто выдал (iss), для кого (sub), до какого времени (exp) и роли (roles).
 * Пароля, телефона и ИИН в токене нет: любой может прочитать середину токена.
 */
@Service
public class TokenService {

    /** Кто выдал токен. Декодер (JwtConfig) отвергает токен с другим iss. */
    public static final String ISSUER = "bloodbank";

    /** Токен живёт 15 минут, не сутки. */
    public static final Duration LIFETIME = Duration.ofMinutes(15);

    private final JwtEncoder encoder;

    public TokenService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String issue(Authentication user) {
        Instant now = Instant.now();

        // у Spring роль называется ROLE_CHIEF_PHYSICIAN, а в токен кладём CHIEF_PHYSICIAN
        List<String> roles = new ArrayList<>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            roles.add(authority.getAuthority().substring("ROLE_".length()));
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getName())
                .issuedAt(now)
                .expiresAt(now.plus(LIFETIME))
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
