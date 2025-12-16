package los.loanapplication.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import los.common.communication.CommunicationStrategy;
import los.common.config.CommunicationMode;
import los.common.dto.*;
import los.loanapplication.entity.LoanApplication;
import los.loanapplication.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationService {
    
    private final LoanApplicationRepository loanApplicationRepository;
    private final CommunicationStrategy communicationStrategy;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${los.communication.mode:SYNC}")
    private CommunicationMode communicationMode;
    
    private final Map<String, CompletableFuture<EligibilityResponseDTO>> eligibilityRequests = new ConcurrentHashMap<>();
    
    @Transactional
    public LoanApplicationDTO createLoanApplication(LoanApplicationDTO loanApplicationDTO) {
        log.info("Creating loan application for customer: {}", loanApplicationDTO.getCustomerId());
        
        LoanApplication application = new LoanApplication();

        application.setCustomerId(loanApplicationDTO.getCustomerId());
        application.setLoanAmount(loanApplicationDTO.getLoanAmount());
        application.setLoanTermMonths(loanApplicationDTO.getLoanTermMonths());
        application.setLoanPurpose(loanApplicationDTO.getLoanPurpose());
        application.setStatus("PENDING");
        application.setApplicationDate(LocalDateTime.now());
        application.setLastUpdated(LocalDateTime.now());
        
        LoanApplication saved = loanApplicationRepository.save(application);
        
        // Trigger eligibility check
        checkEligibilityAsync(saved.getId(), loanApplicationDTO);
        
        return convertToDTO(saved);
    }
    
    private void checkEligibilityAsync(Long applicationId, LoanApplicationDTO loanApplicationDTO) {
        EligibilityRequestDTO eligibilityRequest = new EligibilityRequestDTO();
        eligibilityRequest.setCustomerId(loanApplicationDTO.getCustomerId());
        eligibilityRequest.setRequestedLoanAmount(loanApplicationDTO.getLoanAmount());
        eligibilityRequest.setLoanTermMonths(loanApplicationDTO.getLoanTermMonths());
        eligibilityRequest.setLoanPurpose(loanApplicationDTO.getLoanPurpose());
        // These would typically come from customer service or request
        eligibilityRequest.setMonthlyIncome(new java.math.BigDecimal("5000"));
        eligibilityRequest.setMonthlyExpenses(new java.math.BigDecimal("2000"));
        
        if (communicationMode == CommunicationMode.SYNC) {
            // Synchronous check using Feign
            checkEligibilitySync(applicationId, eligibilityRequest);
        } else {
            // Asynchronous check using Kafka
            checkEligibilityAsyncKafka(applicationId, eligibilityRequest);
        }
    }
    
    /**
     * SYNC mode eligibility check with Circuit Breaker and Retry
     */
    @CircuitBreaker(name = "eligibilityService", fallbackMethod = "checkEligibilitySyncFallback")
    @Retry(name = "eligibilityService", fallbackMethod = "checkEligibilitySyncFallback")
    @Bulkhead(name = "eligibilityService")
    private void checkEligibilitySync(Long applicationId, EligibilityRequestDTO request) {
        log.info("Checking eligibility synchronously for application: {} (with Circuit Breaker)", applicationId);
        
        updateApplicationStatus(applicationId, "ELIGIBILITY_CHECK");
        
        // In sync mode, we need to call eligibility service directly via Feign client
        EligibilityResponseDTO response = communicationStrategy.checkEligibility(request);
        
        updateApplicationWithEligibilityResult(applicationId, response);
    }
    
    /**
     * Fallback for SYNC mode when eligibility service is unavailable
     */
    private void checkEligibilitySyncFallback(Long applicationId, EligibilityRequestDTO request, Exception ex) {
        log.warn("SYNC Circuit Breaker fallback triggered for application: {}. Error: {}", 
                applicationId, ex.getMessage());
        
        EligibilityResponseDTO fallbackResponse = createFallbackEligibilityResponse(request.getCustomerId());
        updateApplicationWithEligibilityResult(applicationId, fallbackResponse);
    }
    
    /**
     * ASYNC mode eligibility check via Kafka with Circuit Breaker and Retry
     */
    @CircuitBreaker(name = "eligibilityServiceKafka", fallbackMethod = "checkEligibilityAsyncKafkaFallback")
    @Retry(name = "kafkaProducer", fallbackMethod = "checkEligibilityAsyncKafkaFallback")
    private void checkEligibilityAsyncKafka(Long applicationId, EligibilityRequestDTO request) {
        log.info("Checking eligibility asynchronously via Kafka for application: {} (with Circuit Breaker)", applicationId);
        
        updateApplicationStatus(applicationId, "ELIGIBILITY_CHECK");
        
        String correlationId = "eligibility-request-" + applicationId + "-" + System.currentTimeMillis();
        EligibilityRequestMessage kafkaMessage = new EligibilityRequestMessage(correlationId, applicationId, request);
        
        // Store the future for when response comes back with timeout handling
        CompletableFuture<EligibilityResponseDTO> future = new CompletableFuture<>();
        eligibilityRequests.put(correlationId, future);
        
        // Send to Kafka with error handling
        kafkaTemplate.send("eligibility-request-topic", correlationId, kafkaMessage)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send Kafka message for application {}: {}", applicationId, ex.getMessage());
                    eligibilityRequests.remove(correlationId);
                    handleKafkaSendFailure(applicationId, request);
                } else {
                    log.info("Eligibility request sent to Kafka: correlationId={}, partition={}, offset={}", 
                            correlationId, 
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        
        // Add timeout handling for async response
        future.orTimeout(30, TimeUnit.SECONDS)
            .exceptionally(ex -> {
                log.error("Eligibility check timed out for application: {}", applicationId);
                eligibilityRequests.remove(correlationId);
                handleEligibilityTimeout(applicationId, request.getCustomerId());
                return null;
            });
        
        log.info("Eligibility request sent to Kafka with correlation ID: {}", correlationId);
    }
    
    /**
     * Fallback for ASYNC/Kafka mode when Kafka is unavailable
     */
    private void checkEligibilityAsyncKafkaFallback(Long applicationId, EligibilityRequestDTO request, Exception ex) {
        log.warn("ASYNC/Kafka Circuit Breaker fallback triggered for application: {}. Error: {}", 
                applicationId, ex.getMessage());
        
        // Mark as processing failed - customer should retry later
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
        
        application.setStatus("PROCESSING_FAILED");
        application.setEligibilityReason("Service temporarily unavailable. Please retry later. Error: " + ex.getMessage());
        application.setLastUpdated(LocalDateTime.now());
        loanApplicationRepository.save(application);
        
        log.info("Application {} marked as PROCESSING_FAILED due to Kafka circuit breaker", applicationId);
    }
    
    /**
     * Handle Kafka send failures
     */
    private void handleKafkaSendFailure(Long applicationId, EligibilityRequestDTO request) {
        log.warn("Handling Kafka send failure for application: {}", applicationId);
        
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
        
        application.setStatus("PROCESSING_FAILED");
        application.setEligibilityReason("Failed to send eligibility request. Please retry later.");
        application.setLastUpdated(LocalDateTime.now());
        loanApplicationRepository.save(application);
    }
    
    /**
     * Handle eligibility check timeout
     */
    private void handleEligibilityTimeout(Long applicationId, Long customerId) {
        log.warn("Handling eligibility timeout for application: {}", applicationId);
        
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
        
        application.setStatus("TIMEOUT");
        application.setEligibilityReason("Eligibility check timed out. Please retry later.");
        application.setLastUpdated(LocalDateTime.now());
        loanApplicationRepository.save(application);
    }
    
    /**
     * Create a conservative fallback eligibility response
     */
    private EligibilityResponseDTO createFallbackEligibilityResponse(Long customerId) {
        EligibilityResponseDTO fallbackResponse = new EligibilityResponseDTO();
        fallbackResponse.setCustomerId(customerId);
        fallbackResponse.setEligible(false);
        fallbackResponse.setEligibleLoanAmount(BigDecimal.ZERO);
        fallbackResponse.setReason("Service temporarily unavailable. Please try again later.");
        fallbackResponse.setRecommendedInterestRate(null);
        fallbackResponse.setRecommendedTermMonths(null);
        return fallbackResponse;
    }
    
    @KafkaListener(topics = "eligibility-response-topic", groupId = "loan-application-service-group")
    public void handleEligibilityResponse(EligibilityResponseMessage message) {
        log.info("Received eligibility response for correlation ID: {}", message.getCorrelationId());
        CompletableFuture<EligibilityResponseDTO> future = eligibilityRequests.remove(message.getCorrelationId());
        
        if (future != null) {
            future.complete(message.getResponse());
            updateApplicationWithEligibilityResult(message.getApplicationId(), message.getResponse());
        }
    }
    
    @Transactional
    private void updateApplicationStatus(Long applicationId, String status) {
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
        application.setStatus(status);
        application.setLastUpdated(LocalDateTime.now());
        loanApplicationRepository.save(application);
    }
    
    @Transactional
    private void updateApplicationWithEligibilityResult(Long applicationId, EligibilityResponseDTO response) {
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
        
        application.setEligible(response.getEligible());
        application.setEligibleLoanAmount(response.getEligibleLoanAmount());
        application.setEligibilityReason(response.getReason());
        application.setRecommendedInterestRate(response.getRecommendedInterestRate());
        application.setRecommendedTermMonths(response.getRecommendedTermMonths());
        application.setStatus(response.getEligible() ? "APPROVED" : "REJECTED");
        application.setLastUpdated(LocalDateTime.now());
        
        loanApplicationRepository.save(application);
        log.info("Updated application {} with eligibility result. Eligible: {}", applicationId, response.getEligible());
    }
    
    public LoanApplicationDTO getLoanApplicationById(Long id) {
        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan application not found with id: " + id));
        return convertToDTO(application);
    }
    
    public List<LoanApplicationDTO> getLoanApplicationsByCustomerId(Long customerId) {
        return loanApplicationRepository.findByCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<LoanApplicationDTO> getAllLoanApplications() {
        return loanApplicationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private LoanApplicationDTO convertToDTO(LoanApplication application) {
        LoanApplicationDTO dto = new LoanApplicationDTO();
        dto.setId(application.getId());
        dto.setCustomerId(application.getCustomerId());
        dto.setLoanAmount(application.getLoanAmount());
        dto.setLoanTermMonths(application.getLoanTermMonths());
        dto.setLoanPurpose(application.getLoanPurpose());
        dto.setStatus(application.getStatus());
        dto.setApplicationDate(application.getApplicationDate());
        dto.setLastUpdated(application.getLastUpdated());
        
        if (application.getEligible() != null) {
            EligibilityResponseDTO eligibilityResponse = new EligibilityResponseDTO();
            eligibilityResponse.setCustomerId(application.getCustomerId());
            eligibilityResponse.setEligible(application.getEligible());
            eligibilityResponse.setEligibleLoanAmount(application.getEligibleLoanAmount());
            eligibilityResponse.setReason(application.getEligibilityReason());
            eligibilityResponse.setRecommendedInterestRate(application.getRecommendedInterestRate());
            eligibilityResponse.setRecommendedTermMonths(application.getRecommendedTermMonths());
            dto.setEligibilityResponse(eligibilityResponse);
        }
        
        return dto;
    }
    
    // Inner classes for Kafka messages
    public static class EligibilityRequestMessage {
        private String correlationId;
        private Long applicationId;
        private EligibilityRequestDTO request;
        
        public EligibilityRequestMessage() {}
        
        public EligibilityRequestMessage(String correlationId, Long applicationId, EligibilityRequestDTO request) {
            this.correlationId = correlationId;
            this.applicationId = applicationId;
            this.request = request;
        }
        
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
