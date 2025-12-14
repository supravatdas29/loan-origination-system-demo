package los.customer.communication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import los.common.communication.CommunicationStrategy;
import los.common.dto.CustomerDTO;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "ASYNC")
@RequiredArgsConstructor
@Slf4j
public class AsyncCommunicationStrategy implements CommunicationStrategy {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Override
    public CustomerDTO getCustomerById(Long customerId) {
        throw new UnsupportedOperationException("Customer service doesn't call other services");
    }

    @Override
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        throw new UnsupportedOperationException("Eligibility check is handled by eligibility-service");
    }
}
