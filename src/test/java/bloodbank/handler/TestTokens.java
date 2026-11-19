package bloodbank.handler;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Поддельные токены для тестов веб-слоя: ничего не подписывается и не проверяется,
 * jwt() просто делает вид, что пришёл проверенный токен с такими ролями.
 * Нет .with(...) в запросе, значит токена нет и вызывающий аноним.
 */
final class TestTokens {

    private TestTokens() {
    }

    /** Лаборант (младшая роль): создаёт и читает, но выдавать пакеты не может. */
    static RequestPostProcessor technician() {
        return jwt().jwt(token -> token.subject("technician"))
                .authorities(new SimpleGrantedAuthority("ROLE_LAB_TECHNICIAN"));
    }

    /** Главврач (старшая роль): может всё, включая выдачу пакета. */
    static RequestPostProcessor chief() {
        return jwt().jwt(token -> token.subject("chief"))
                .authorities(new SimpleGrantedAuthority("ROLE_CHIEF_PHYSICIAN"));
    }
}
