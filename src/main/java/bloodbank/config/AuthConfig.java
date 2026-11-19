package bloodbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Кто может войти: пароли, список пользователей и проверка "имя + пароль".
 * Таблицы пользователей в этой лабе нет, поэтому dev-пользователи лежат в памяти.
 */
@Configuration
public class AuthConfig {

    /** BCrypt: пароль превращается в хэш, обратно из хэша пароль не достать. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Два пользователя только для ноутбука (профиль dev). Пароли храним хэшем, не текстом.
     * User.roles("X") сам превращает роль в ROLE_X.
     */
    @Bean
    @Profile("dev")
    UserDetailsService devUsers(PasswordEncoder encoder) {
        UserDetails technician = User.withUsername("technician")
                .password(encoder.encode("tech-dev"))
                .roles("LAB_TECHNICIAN")
                .build();
        UserDetails chief = User.withUsername("chief")
                .password(encoder.encode("chief-dev"))
                .roles("CHIEF_PHYSICIAN")
                .build();
        return new InMemoryUserDetailsManager(technician, chief);
    }

    /** В проде dev-пользователей нет. Пока пустой список: войти нельзя, пользователи будут позже из таблицы. */
    @Bean
    @Profile("!dev")
    UserDetailsService noUsers() {
        return new InMemoryUserDetailsManager();
    }

    /** "Проверь имя и пароль": находит пользователя в списке и сравнивает хэши. */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
}
