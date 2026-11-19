package bloodbank.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Цепочка фильтров: стоит перед контроллером и проверяет каждый запрос.
 * Правила идут сверху вниз, срабатывает ПЕРВОЕ подходящее. Порядок как в таблице README.
 * Узкое правило (выдача пакета) обязано стоять выше широкого /api/v1/**, иначе лаборант получит 200.
 */
@Configuration
@EnableMethodSecurity // включает @PreAuthorize на методах сервиса (второй замок)
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http, ObjectMapper mapper) throws Exception {
        SecurityErrors errors = new SecurityErrors(mapper);

        return http
                .authorizeHttpRequests(rules -> rules
                        // 1. вход: сюда приходят за токеном, токена ещё нет
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        // 2. проверка "жив ли сервис"
                        .requestMatchers("/actuator/health").permitAll()
                        // 3. наша страница входа и её css и js (только эти адреса, /api сюда не попадает)
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/*.css", "/*.js",
                                "/css/**", "/js/**").permitAll()
                        // 4. Swagger: описание API открыто без токена
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 5. закрытое действие: только главврач
                        .requestMatchers(HttpMethod.POST, "/api/v1/blood-units/*/issue").hasRole("CHIEF_PHYSICIAN")
                        // 6. остальное API и "кто я": обе роли
                        .requestMatchers("/api/v1/**", "/api/me").hasAnyRole("LAB_TECHNICIAN", "CHIEF_PHYSICIAN")
                        // 7. всё, что не перечислено выше, запрещено
                        .anyRequest().denyAll())
                // каждый запрос должен принести Authorization: Bearer <токен>, проверяет его JwtDecoder
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesFromToken()))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                // без кук и сессий: токен приходит в заголовке, поэтому CSRF не нужен
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    /**
     * Говорим Spring, где в токене роли: в поле "roles", и к каждой добавить ROLE_.
     * Без этого hasRole("CHIEF_PHYSICIAN") не нашёл бы роль и главврач получил бы 403.
     */
    private JwtAuthenticationConverter rolesFromToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
