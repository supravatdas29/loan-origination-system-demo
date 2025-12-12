package los.loanapplication.communication;

import los.common.communication.CommunicationStrategy;
import los.common.dto.CustomerDTO;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "ASYNC")
@Slf4j
public class AsyncCommunicationStrategy implements CommunicationStrategy {
    
    @Override
    public CustomerDTO getCustomerById(Long customerId) {
        throw new UnsupportedOperationException("Customer lookup should be done via customer service");
    }
    
    @Override
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        // In async mode, eligibility check is handled via Kafka
        // This method should not be called directly
        log.warn("Direct eligibility check called in async mode. Should use Kafka instead.");
        throw new UnsupportedOperationException("Eligibility check in async mode should be done via Kafka");
    }
}
