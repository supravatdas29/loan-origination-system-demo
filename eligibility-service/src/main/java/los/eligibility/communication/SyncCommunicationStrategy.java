package los.eligibility.communication;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
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
    
    /**
     * Get customer by ID via Feign with Circuit Breaker and Retry
     * Now fetches customer WITH civil score for eligibility decisions
     */
    @Override
    @CircuitBreaker(name = "customerService", fallbackMethod = "getCustomerByIdFallback")
    @Retry(name = "customerService", fallbackMethod = "getCustomerByIdFallback")
    @Bulkhead(name = "customerService")
    public CustomerDTO getCustomerById(Long customerId) {
        log.info("Fetching customer {} with civil score via Feign client (SYNC) with Circuit Breaker", customerId);
        try {
            // Use the new endpoint that fetches customer with civil score
            CustomerDTO customer = customerServiceClient.getCustomerWithCivilScore(customerId);
            log.info("Customer {} civil score: {} ({})", customerId, 
                    customer.getCivilScore(), customer.getCivilScoreCategory());
            return customer;
        } catch (Exception e) {
            log.error("Error fetching customer via Feign: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch customer: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fallback method when customer service is unavailable
     */
    public CustomerDTO getCustomerByIdFallback(Long customerId, Exception ex) {
        log.warn("Customer Service Circuit Breaker fallback triggered for customer: {}. Error: {}", 
                customerId, ex.getMessage());
        
        // Return a fallback customer with default values
        CustomerDTO fallbackCustomer = new CustomerDTO();
        fallbackCustomer.setId(customerId);
        fallbackCustomer.setName("Unknown Customer (Fallback)");
        fallbackCustomer.setEmail("unknown@fallback.com");
        fallbackCustomer.setPhone("000-000-0000");
        
        log.info("Returning fallback customer for ID: {}", customerId);
        return fallbackCustomer;
    }
    
    @Override
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        // This method is not used in eligibility service context
        // Eligibility check is handled by the service itself
        throw new UnsupportedOperationException("Eligibility check should be called directly on service");
    }
}
