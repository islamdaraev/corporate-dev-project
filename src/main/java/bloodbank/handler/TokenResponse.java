package bloodbank.handler;

/** Ответ на вход: сам токен, его тип (всегда Bearer) и сколько секунд он живёт (900 = 15 минут). */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
