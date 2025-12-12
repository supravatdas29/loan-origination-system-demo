package los.eligibility.communication;

import los.common.communication.CommunicationStrategy;
import los.common.dto.CustomerDTO;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "ASYNC")
@RequiredArgsConstructor
@Slf4j
public class AsyncCommunicationStrategy implements CommunicationStrategy {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, CompletableFuture<CustomerDTO>> customerRequests = new ConcurrentHashMap<>();
    
    @Override
    public CustomerDTO getCustomerById(Long customerId) {
        log.info("Requesting customer {} via Kafka (ASYNC)", customerId);
        
        String correlationId = "customer-request-" + System.currentTimeMillis() + "-" + customerId;
        CompletableFuture<CustomerDTO> future = new CompletableFuture<>();
        customerRequests.put(correlationId, future);
        
        // Send request to Kafka
        CustomerRequestMessage request = new CustomerRequestMessage(correlationId, customerId);
        kafkaTemplate.send("customer-request-topic", correlationId, request);
        
        try {
            // Wait for response (with timeout of 30 seconds)
            CustomerDTO customer = future.get(30, TimeUnit.SECONDS);
            if (customer == null) {
                throw new RuntimeException("Customer not found or error occurred");
            }
            return customer;
        } catch (Exception e) {
            log.error("Error fetching customer via Kafka: {}", e.getMessage());
            customerRequests.remove(correlationId);
            throw new RuntimeException("Failed to fetch customer: " + e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = "customer-response-topic", groupId = "eligibility-service-group")
    public void handleCustomerResponse(CustomerResponseMessage response) {
        log.info("Received customer response for correlation ID: {}", response.getCorrelationId());
        CompletableFuture<CustomerDTO> future = customerRequests.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response.getCustomer());
        }
    }
    
    @Override
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        throw new UnsupportedOperationException("Eligibility check should be called directly on service");
    }
    
    // Inner classes for Kafka messages
    public static class CustomerRequestMessage {
        private String correlationId;
        private Long customerId;
        
        public CustomerRequestMessage() {}
        
        public CustomerRequestMessage(String correlationId, Long customerId) {
            this.correlationId = correlationId;
            this.customerId = customerId;
        }
        
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
    }
    
    public static class CustomerResponseMessage {
        private String correlationId;
        private CustomerDTO customer;
        
        public CustomerResponseMessage() {}
        
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        public CustomerDTO getCustomer() { return customer; }
        public void setCustomer(CustomerDTO customer) { this.customer = customer; }
    }
}
