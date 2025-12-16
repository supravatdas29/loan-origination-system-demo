package los.loanapplication.communication;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import los.common.communication.CommunicationStrategy;
import los.common.dto.CustomerDTO;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import los.loanapplication.client.EligibilityServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
    
    /**
     * Check eligibility via Feign with Circuit Breaker and Retry
     */
    @Override
    @CircuitBreaker(name = "eligibilityService", fallbackMethod = "checkEligibilityFallback")
    @Retry(name = "eligibilityService", fallbackMethod = "checkEligibilityFallback")
    @Bulkhead(name = "eligibilityService")
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        log.info("Checking eligibility via Feign client (SYNC) with Circuit Breaker for customer: {}", request.getCustomerId());
        try {
            return eligibilityServiceClient.checkEligibility(request);
        } catch (Exception e) {
            log.error("Error checking eligibility via Feign: {}", e.getMessage());
            throw new RuntimeException("Failed to check eligibility: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fallback method when eligibility service is unavailable
     */
    public EligibilityResponseDTO checkEligibilityFallback(EligibilityRequestDTO request, Exception ex) {
        log.warn("Eligibility Service Circuit Breaker fallback triggered for customer: {}. Error: {}", 
                request.getCustomerId(), ex.getMessage());
        
        // Return a conservative fallback response - not eligible
        EligibilityResponseDTO fallbackResponse = new EligibilityResponseDTO();
        fallbackResponse.setCustomerId(request.getCustomerId());
        fallbackResponse.setEligible(false);
        fallbackResponse.setEligibleLoanAmount(BigDecimal.ZERO);
        fallbackResponse.setReason("Service temporarily unavailable. Please try again later. Error: " + ex.getMessage());
        fallbackResponse.setRecommendedInterestRate(null);
        fallbackResponse.setRecommendedTermMonths(null);
        
        log.info("Returning fallback eligibility response for customer: {}", request.getCustomerId());
        return fallbackResponse;
    }
}
