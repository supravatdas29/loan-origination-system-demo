package los.common.communication;

import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import los.common.dto.CustomerDTO;

/**
 * Strategy interface for inter-service communication
 * Supports both synchronous (Feign) and asynchronous (Kafka) implementations
 */
public interface CommunicationStrategy {
    
    /**
     * Get customer information by ID
     */
    CustomerDTO getCustomerById(Long customerId);
    
    /**
     * Check customer eligibility for loan
     * Returns EligibilityResponseDTO for sync, void for async (handled via callback)
     */
    EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request);
}
