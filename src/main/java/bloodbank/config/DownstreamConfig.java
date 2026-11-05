package bloodbank.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Включает привязку yaml к DownstreamProperties. */
@Configuration
@EnableConfigurationProperties(DownstreamProperties.class)
public class DownstreamConfig {
}