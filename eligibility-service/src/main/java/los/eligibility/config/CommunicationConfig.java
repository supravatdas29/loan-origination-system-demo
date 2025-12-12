package los.eligibility.config;

import los.common.communication.CommunicationStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CommunicationConfig {
    
    // This will be conditionally created based on property
    // Sync and Async strategies use @ConditionalOnProperty
    // This bean ensures CommunicationStrategy is always available
    @Bean
    @Primary
    @ConditionalOnMissingBean(CommunicationStrategy.class)
    public CommunicationStrategy defaultCommunicationStrategy() {
        // This should not be used as we expect one of the conditional beans
        throw new IllegalStateException("No communication strategy configured. Set los.communication.mode to SYNC or ASYNC");
    }
}
