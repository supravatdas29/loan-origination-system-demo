package los.loanapplication.config;

import los.common.communication.CommunicationStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CommunicationConfig {
    
    @Bean
    @Primary
    @ConditionalOnMissingBean(CommunicationStrategy.class)
    public CommunicationStrategy defaultCommunicationStrategy() {
        throw new IllegalStateException("No communication strategy configured. Set los.communication.mode to SYNC or ASYNC");
    }
}
