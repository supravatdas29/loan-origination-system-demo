package los.customer.communication;

import los.common.communication.CommunicationStrategy;
import los.common.dto.CustomerDTO;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Minimal Async communication strategy for customer-service.
 * The customer-service is primarily a provider of customer data
 * so it does not call other services; this implementation exists
 * so a CommunicationStrategy bean can be created when ASYNC mode
 * is active. Methods throw UnsupportedOperationException when used.
 */
public class AsyncCommunicationStrategy implements CommunicationStrategy {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AsyncCommunicationStrategy(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CustomerDTO getCustomerById(Long customerId) {
        throw new UnsupportedOperationException("Customer service does not call other services");
    }

    @Override
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        throw new UnsupportedOperationException("Eligibility checks are handled by eligibility-service");
    }
}
