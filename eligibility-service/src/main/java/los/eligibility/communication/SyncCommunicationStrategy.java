package los.eligibility.communication;

import los.common.communication.CommunicationStrategy;
import los.common.dto.CustomerDTO;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import los.eligibility.client.CustomerServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "SYNC")
@RequiredArgsConstructor
@Slf4j
public class SyncCommunicationStrategy implements CommunicationStrategy {
    
    private final CustomerServiceClient customerServiceClient;
    
    @Override
    public CustomerDTO getCustomerById(Long customerId) {
        log.info("Fetching customer {} via Feign client (SYNC)", customerId);
        try {
            return customerServiceClient.getCustomerById(customerId);
        } catch (Exception e) {
            log.error("Error fetching customer via Feign: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch customer: " + e.getMessage(), e);
        }
    }
    
    @Override
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        // This method is not used in eligibility service context
        // Eligibility check is handled by the service itself
        throw new UnsupportedOperationException("Eligibility check should be called directly on service");
    }
}
