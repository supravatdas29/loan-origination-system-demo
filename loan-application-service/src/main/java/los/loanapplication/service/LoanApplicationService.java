package los.loanapplication.service;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    
    private void checkEligibilitySync(Long applicationId, EligibilityRequestDTO request) {
        log.info("Checking eligibility synchronously for application: {}", applicationId);
        
        updateApplicationStatus(applicationId, "ELIGIBILITY_CHECK");
        
        // In sync mode, we need to call eligibility service directly
        // This would typically be done via a Feign client
        EligibilityResponseDTO response = communicationStrategy.checkEligibility(request);
        
        updateApplicationWithEligibilityResult(applicationId, response);
    }
    
    private void checkEligibilityAsyncKafka(Long applicationId, EligibilityRequestDTO request) {
        log.info("Checking eligibility asynchronously via Kafka for application: {}", applicationId);
        
        updateApplicationStatus(applicationId, "ELIGIBILITY_CHECK");
        
        String correlationId = "eligibility-request-" + applicationId + "-" + System.currentTimeMillis();
        EligibilityRequestMessage kafkaMessage = new EligibilityRequestMessage(correlationId, applicationId, request);
        
        // Store the future for when response comes back
        CompletableFuture<EligibilityResponseDTO> future = new CompletableFuture<>();
        eligibilityRequests.put(correlationId, future);
        
        // Send to Kafka
        kafkaTemplate.send("eligibility-request-topic", correlationId, kafkaMessage);
        
        log.info("Eligibility request sent to Kafka with correlation ID: {}", correlationId);
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
