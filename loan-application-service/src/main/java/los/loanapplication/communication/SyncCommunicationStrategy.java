package los.loanapplication.communication;

import los.common.communication.CommunicationStrategy;
import los.common.dto.CustomerDTO;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import los.loanapplication.client.EligibilityServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "SYNC")
@RequiredArgsConstructor
@Slf4j
public class SyncCommunicationStrategy implements CommunicationStrategy {
    
    private final EligibilityServiceClient eligibilityServiceClient;
    
    @Override
    public CustomerDTO getCustomerById(Long customerId) {
        // Not used in loan application service context
        throw new UnsupportedOperationException("Customer lookup should be done via customer service");
    }
    
    @Override
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        log.info("Checking eligibility via Feign client (SYNC) for customer: {}", request.getCustomerId());
        try {
            return eligibilityServiceClient.checkEligibility(request);
        } catch (Exception e) {
            log.error("Error checking eligibility via Feign: {}", e.getMessage());
            throw new RuntimeException("Failed to check eligibility: " + e.getMessage(), e);
        }
    }
}
