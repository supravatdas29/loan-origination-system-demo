package los.customer.config;

import los.common.communication.CommunicationStrategy;
import los.customer.communication.AsyncCommunicationStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CommunicationConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(CommunicationStrategy.class)
    @ConditionalOnProperty(name = "los.communication.mode", havingValue = "ASYNC")
    public CommunicationStrategy asyncCommunicationStrategy(
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        return new AsyncCommunicationStrategy(kafkaTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(CommunicationStrategy.class)
    public CommunicationStrategy defaultCommunicationStrategy() {
        throw new IllegalStateException("No communication strategy configured. Set los.communication.mode to SYNC or ASYNC");
    }

}
