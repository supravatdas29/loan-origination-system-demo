package los.eligibility.service;

import los.common.communication.CommunicationStrategy;
import los.common.config.CommunicationMode;
import los.common.dto.CustomerDTO;
import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class EligibilityService {
    
    private final CommunicationStrategy communicationStrategy;
    
    @Value("${los.communication.mode:SYNC}")
    private CommunicationMode communicationMode;
    
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        log.info("Checking eligibility for customer ID: {}", request.getCustomerId());
        
        // Get customer details
        CustomerDTO customer = communicationStrategy.getCustomerById(request.getCustomerId());
        
        if (customer == null) {
            return createEligibilityResponse(request.getCustomerId(), false, 
                BigDecimal.ZERO, "Customer not found", null, null);
        }
        
        // Business logic for eligibility check
        BigDecimal debtToIncomeRatio = calculateDebtToIncomeRatio(
            request.getMonthlyIncome(), 
            request.getMonthlyExpenses()
        );
        
        // Eligibility criteria
        boolean eligible = debtToIncomeRatio.compareTo(new BigDecimal("0.43")) <= 0 &&
                          request.getRequestedLoanAmount().compareTo(new BigDecimal("500000")) <= 0;
        
        BigDecimal eligibleAmount = BigDecimal.ZERO;
        BigDecimal interestRate = null;
        Integer recommendedTerm = null;
        
        if (eligible) {
            // Calculate eligible amount (typically 2-3x monthly income)
            eligibleAmount = request.getMonthlyIncome()
                .multiply(new BigDecimal("36")) // 3 years of income
                .min(request.getRequestedLoanAmount());
            
            // Calculate interest rate based on debt-to-income ratio
            if (debtToIncomeRatio.compareTo(new BigDecimal("0.30")) <= 0) {
                interestRate = new BigDecimal("5.5");
            } else if (debtToIncomeRatio.compareTo(new BigDecimal("0.35")) <= 0) {
                interestRate = new BigDecimal("6.5");
            } else {
                interestRate = new BigDecimal("7.5");
            }
            
            recommendedTerm = request.getLoanTermMonths() != null ? 
                request.getLoanTermMonths() : 60;
        }
        
        String reason = eligible ? 
            "Customer meets eligibility criteria" : 
            "Debt-to-income ratio too high or requested amount exceeds limit";
        
        EligibilityResponseDTO response = createEligibilityResponse(
            request.getCustomerId(),
            eligible,
            eligibleAmount,
            reason,
            interestRate,
            recommendedTerm
        );
        
        log.info("Eligibility check completed. Eligible: {}", eligible);
        return response;
    }
    
    private BigDecimal calculateDebtToIncomeRatio(BigDecimal monthlyIncome, BigDecimal monthlyExpenses) {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("1.0"); // Worst case
        }
        if (monthlyExpenses == null) {
            monthlyExpenses = BigDecimal.ZERO;
        }
        return monthlyExpenses.divide(monthlyIncome, 2, RoundingMode.HALF_UP);
    }
    
    private EligibilityResponseDTO createEligibilityResponse(
            Long customerId, Boolean eligible, BigDecimal eligibleAmount, 
            String reason, BigDecimal interestRate, Integer recommendedTerm) {
        EligibilityResponseDTO response = new EligibilityResponseDTO();
        response.setCustomerId(customerId);
        response.setEligible(eligible);
        response.setEligibleLoanAmount(eligibleAmount);
        response.setReason(reason);
        response.setRecommendedInterestRate(interestRate);
        response.setRecommendedTermMonths(recommendedTerm);
        return response;
    }
}
