package bloodbank.handler;

import bloodbank.config.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Единственное место, где мы видим пароль. Проверили имя и пароль, выдали токен на 15 минут.
 * Неверное имя и неверный пароль дают ОДИН ответ (см. ApiErrors), чтобы не подсказать,
 * какие имена существуют.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authentication;
    private final TokenService tokens;

    public AuthController(AuthenticationManager authentication, TokenService tokens) {
        this.authentication = authentication;
        this.tokens = tokens;
    }

    @Operation(summary = "Войти и получить токен", description = "Токен действует 15 минут. Дальше: заголовок Authorization: Bearer <токен>")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токен выдан"),
            @ApiResponse(responseCode = "400", description = "Имя или пароль пустые"),
            @ApiResponse(responseCode = "401", description = "Неверное имя или пароль")
    })
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest body) {
        Authentication user = authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        return new TokenResponse(tokens.issue(user), "Bearer", TokenService.LIFETIME.toSeconds());
    }
}
