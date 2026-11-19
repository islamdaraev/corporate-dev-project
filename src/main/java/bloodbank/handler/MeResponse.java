package bloodbank.handler;

import java.util.List;

/** Кто звонит: имя и роли (например ROLE_CHIEF_PHYSICIAN). Берётся из токена этого запроса. */
public record MeResponse(String username, List<String> authorities) {
}
