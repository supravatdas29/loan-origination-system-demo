package los.customer.config;

import los.common.communication.CommunicationStrategy;
import los.common.dto.CustomerDTO;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
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
    @ConditionalOnProperty(name = "los.communication.mode", havingValue = "ASYNC")
    public CommunicationStrategy asyncCommunicationStrategy(
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        return new AsyncCommunicationStrategy(kafkaTemplate);
    }
    
    /**
     * No-op communication strategy for SYNC mode.
     * Customer service doesn't need to call other services - it only responds to requests.
     */
    @Bean
    @ConditionalOnProperty(name = "los.communication.mode", havingValue = "SYNC", matchIfMissing = true)
    public CommunicationStrategy syncCommunicationStrategy() {
        return new CommunicationStrategy() {
            @Override
            public CustomerDTO getCustomerById(Long customerId) {
                throw new UnsupportedOperationException("Customer service does not call other services");
            }
            
            @Override
            public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
                throw new UnsupportedOperationException("Customer service does not call other services");
            }
        };
    }
}