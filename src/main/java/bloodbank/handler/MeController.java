package bloodbank.handler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Кто я. Authentication положил в запрос фильтр после проверки токена.
 * Роль страница берёт отсюда, а не из тела запроса, где можно написать что угодно.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
public class MeController {

    @Operation(summary = "Кто я: имя и роли из токена")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Имя и роли"),
            @ApiResponse(responseCode = "401", description = "Токена нет, он битый или просрочен")
    })
    @GetMapping("/api/me")
    public MeResponse me(Authentication authentication) {
        List<String> authorities = new ArrayList<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            authorities.add(authority.getAuthority());
        }
        return new MeResponse(authentication.getName(), authorities);
    }
}
