package los.eligibility.service;

import los.common.config.CommunicationMode;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
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
public class EligibilityKafkaConsumer {
    
    private final EligibilityService eligibilityService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @KafkaListener(topics = "eligibility-request-topic", groupId = "eligibility-service-group")
    public void handleEligibilityRequest(EligibilityRequestMessage message) {
        log.info("Received eligibility request via Kafka: correlationId={}, applicationId={}", 
                message.getCorrelationId(), message.getApplicationId());
        
        try {
            EligibilityResponseDTO response = eligibilityService.checkEligibility(message.getRequest());
            
            // Send response back via Kafka
            EligibilityResponseMessage responseMessage = new EligibilityResponseMessage();
            responseMessage.setCorrelationId(message.getCorrelationId());
            responseMessage.setApplicationId(message.getApplicationId());
            responseMessage.setResponse(response);
            
            kafkaTemplate.send("eligibility-response-topic", message.getCorrelationId(), responseMessage);
            log.info("Sent eligibility response via Kafka: correlationId={}", message.getCorrelationId());
        } catch (Exception e) {
            log.error("Error processing eligibility request: {}", e.getMessage(), e);
            // Send error response
            EligibilityResponseDTO errorResponse = new EligibilityResponseDTO();
            errorResponse.setCustomerId(message.getRequest().getCustomerId());
            errorResponse.setEligible(false);
            errorResponse.setReason("Error processing eligibility: " + e.getMessage());
            
            EligibilityResponseMessage responseMessage = new EligibilityResponseMessage();
            responseMessage.setCorrelationId(message.getCorrelationId());
            responseMessage.setApplicationId(message.getApplicationId());
            responseMessage.setResponse(errorResponse);
            
            kafkaTemplate.send("eligibility-response-topic", message.getCorrelationId(), responseMessage);
        }
    }
    
    // Inner classes for Kafka messages
    public static class EligibilityRequestMessage {
        private String correlationId;
        private Long applicationId;
        private EligibilityRequestDTO request;
        
        public EligibilityRequestMessage() {}
        
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        public Long getApplicationId() { return applicationId; }
        public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
        public EligibilityRequestDTO getRequest() { return request; }
        public void setRequest(EligibilityRequestDTO request) { this.request = request; }
    }
    
    public static class EligibilityResponseMessage {
        private String correlationId;
        private Long applicationId;
        private EligibilityResponseDTO response;
        
        public EligibilityResponseMessage() {}
        
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        public Long getApplicationId() { return applicationId; }
        public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
        public EligibilityResponseDTO getResponse() { return response; }
        public void setResponse(EligibilityResponseDTO response) { this.response = response; }
    }
}
