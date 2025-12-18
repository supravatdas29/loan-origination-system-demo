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

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import los.common.messaging.CustomerRequestMessage;
import los.common.messaging.CustomerResponseMessage;

@Component
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "ASYNC")
@RequiredArgsConstructor
@Slf4j
public class AsyncCommunicationStrategy implements CommunicationStrategy {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, CompletableFuture<CustomerDTO>> customerRequests = new ConcurrentHashMap<>();

    /**
     * Get customer by ID via Kafka (ASYNC)
     * Note: Kafka handles broker failures internally. We use timeout + fallback for response handling.
     */
    @Override
    public CustomerDTO getCustomerById(Long customerId) {
        log.info("Requesting customer {} via Kafka (ASYNC)", customerId);

        String correlationId = "customer-request-" + System.currentTimeMillis() + "-" + customerId;
        CompletableFuture<CustomerDTO> future = new CompletableFuture<>();
        customerRequests.put(correlationId, future);

        // Send request to Kafka
        CustomerRequestMessage request = new CustomerRequestMessage(correlationId, customerId);
        kafkaTemplate.send("customer-request-topic", correlationId, request)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka request send failed for key={}: {}", correlationId, ex.getMessage(), ex);
                        customerRequests.remove(correlationId);
                        future.completeExceptionally(ex);
                    } else if (result != null && result.getRecordMetadata() != null) {
                        log.info("Kafka request sent: topic={}, partition={}, offset={}, key={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                correlationId);
                    } else {
                        log.info("Kafka request sent with no metadata for key={}", correlationId);
                    }
                });

        try {
            // Wait for response (with timeout of 30 seconds)
            CustomerDTO customer = future.get(30, TimeUnit.SECONDS);
            if (customer == null) {
                log.warn("Customer not found, using fallback for customer: {}", customerId);
                return createFallbackCustomer(customerId, "Customer not found");
            }
            return customer;
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("Timeout waiting for customer {} response from Kafka", customerId);
            customerRequests.remove(correlationId);
            return createFallbackCustomer(customerId, "Request timed out");
        } catch (Exception e) {
            log.error("Error fetching customer via Kafka: {}", e.getMessage());
            customerRequests.remove(correlationId);
            return createFallbackCustomer(customerId, e.getMessage());
        }
    }

    /**
     * Create fallback customer when Kafka communication fails or times out
     */
    private CustomerDTO createFallbackCustomer(Long customerId, String reason) {
        log.warn("Creating fallback customer for ID: {}. Reason: {}", customerId, reason);
        
        CustomerDTO fallbackCustomer = new CustomerDTO();
        fallbackCustomer.setId(customerId);
        fallbackCustomer.setName("Unknown Customer (Service Unavailable)");
        fallbackCustomer.setEmail("unknown@fallback.com");
        fallbackCustomer.setPhone("000-000-0000");
        
        return fallbackCustomer;
    }

    @KafkaListener(topics = "customer-response-topic", groupId = "eligibility-service-group", containerFactory = "customerResponseKafkaListenerContainerFactory")
    public void handleCustomerResponse(CustomerResponseMessage response) {
        log.info("Received customer response(Async:Kafka) for correlation ID: {}", response.getCorrelationId());
        CompletableFuture<CustomerDTO> future = customerRequests.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response.getCustomer());
        }
    }

    @Override
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        throw new UnsupportedOperationException("Eligibility check should be called directly on service");
    }

    // Using shared message DTOs from common-module: CustomerRequestMessage & CustomerResponseMessage
}
