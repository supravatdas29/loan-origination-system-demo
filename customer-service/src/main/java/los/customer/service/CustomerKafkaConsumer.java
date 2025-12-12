package los.customer.service;

import los.common.config.CommunicationMode;
import los.common.dto.CustomerDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    @KafkaListener(topics = "customer-request-topic", groupId = "customer-service-group")
    public void handleCustomerRequest(CustomerRequestMessage message) {
        log.info("Received customer request via Kafka: correlationId={}, customerId={}", 
                message.getCorrelationId(), message.getCustomerId());
        
        try {
            CustomerDTO customer = customerService.getCustomerById(message.getCustomerId());
            
            // Send response back via Kafka
            CustomerResponseMessage responseMessage = new CustomerResponseMessage();
            responseMessage.setCorrelationId(message.getCorrelationId());
            responseMessage.setCustomer(customer);
            
            kafkaTemplate.send("customer-response-topic", message.getCorrelationId(), responseMessage);
            log.info("Sent customer response via Kafka: correlationId={}", message.getCorrelationId());
        } catch (Exception e) {
            log.error("Error processing customer request: {}", e.getMessage(), e);
            // Send error response
            CustomerResponseMessage responseMessage = new CustomerResponseMessage();
            responseMessage.setCorrelationId(message.getCorrelationId());
            responseMessage.setCustomer(null); // null indicates error
            
            kafkaTemplate.send("customer-response-topic", message.getCorrelationId(), responseMessage);
        }
    }
    
    // Inner classes for Kafka messages
    public static class CustomerRequestMessage {
        private String correlationId;
        private Long customerId;
        
        public CustomerRequestMessage() {}
        
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
