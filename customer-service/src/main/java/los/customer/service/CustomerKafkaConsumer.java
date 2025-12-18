package los.customer.service;

import los.common.config.CommunicationMode;
import los.common.dto.CustomerDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import los.common.messaging.CustomerRequestMessage;
import los.common.messaging.CustomerResponseMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "ASYNC")
@RequiredArgsConstructor
@Slf4j
public class CustomerKafkaConsumer {
    
    private final CustomerService customerService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @PostConstruct
    public void init() {
        log.info("=== CustomerKafkaConsumer INITIALIZED - Listening on 'customer-request-topic' ===");
    }
    
    @KafkaListener(topics = "customer-request-topic", groupId = "customer-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void handleCustomerRequest(CustomerRequestMessage message) {
        log.info("Received customer request via Kafka: correlationId={}, customerId={}", 
                message.getCorrelationId(), message.getCustomerId());
        
        try {
            // Fetch customer with civil score for eligibility decisions
            CustomerDTO customer = customerService.getCustomerWithCivilScore(message.getCustomerId());
            log.info("Customer {} civil score: {} ({})", message.getCustomerId(), 
                    customer.getCivilScore(), customer.getCivilScoreCategory());
            
            // Send response back via Kafka
            CustomerResponseMessage responseMessage = new CustomerResponseMessage();
            responseMessage.setCorrelationId(message.getCorrelationId());
            responseMessage.setCustomer(customer);
            
            kafkaTemplate.send("customer-response-topic", message.getCorrelationId(), responseMessage)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka response send failed for key={}: {}", message.getCorrelationId(), ex.getMessage(), ex);
                    } else if (result != null && result.getRecordMetadata() != null) {
                        log.info("Kafka response sent: topic={}, partition={}, offset={}, key={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                message.getCorrelationId());
                    } else {
                        log.info("Kafka response sent with no metadata for key={}", message.getCorrelationId());
                    }
                });
        } catch (Exception e) {
            log.error("Error processing customer request: {}", e.getMessage(), e);
            // Send error response
            CustomerResponseMessage responseMessage = new CustomerResponseMessage();
            responseMessage.setCorrelationId(message.getCorrelationId());
            responseMessage.setCustomer(null); // null indicates error
            
            kafkaTemplate.send("customer-response-topic", message.getCorrelationId(), responseMessage)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka error response send failed for key={}: {}", message.getCorrelationId(), ex.getMessage(), ex);
                    } else if (result != null && result.getRecordMetadata() != null) {
                        log.info("Kafka error response sent: topic={}, partition={}, offset={}, key={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                message.getCorrelationId());
                    } else {
                        log.info("Kafka error response sent with no metadata for key={}", message.getCorrelationId());
                    }
                });
        }
    }
    
    // Using shared message DTOs from common-module: CustomerRequestMessage & CustomerResponseMessage
}
