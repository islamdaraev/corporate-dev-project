package bloodbank.config;

import bloodbank.domain.FinalStatusRule;
import bloodbank.domain.Rule;
import bloodbank.domain.RuleChain;
import bloodbank.domain.TransitionRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleConfig {

    @Bean
    public Rule rule() {
        return new RuleChain(new TransitionRule(), new FinalStatusRule());
    }
}

