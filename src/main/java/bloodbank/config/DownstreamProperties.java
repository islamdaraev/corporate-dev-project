package bloodbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Адрес другой команды и таймаут. Значения берутся из yaml, в Java их нет. */
@ConfigurationProperties(prefix = "app.downstream")
public record DownstreamProperties(String baseUrl, Duration timeout) {
}
