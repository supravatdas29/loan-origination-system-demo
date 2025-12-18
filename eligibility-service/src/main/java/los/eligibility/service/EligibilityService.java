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
    
    // Civil Score thresholds
    private static final int MINIMUM_CIVIL_SCORE = 580;  // Below this = automatic rejection
    private static final int GOOD_CIVIL_SCORE = 670;     // Good credit
    private static final int EXCELLENT_CIVIL_SCORE = 740; // Excellent credit
    
    public EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request) {
        log.info("Checking eligibility for customer ID: {}", request.getCustomerId());
        
        // Get customer details (now includes civil score)
        CustomerDTO customer = communicationStrategy.getCustomerById(request.getCustomerId());
        
        if (customer == null) {
            return createEligibilityResponse(request.getCustomerId(), false, 
                BigDecimal.ZERO, "Customer not found", null, null);
        }
        
        // Check civil score first - this is a hard requirement
        Integer civilScore = customer.getCivilScore();
        if (civilScore == null || civilScore == 0) {
            log.warn("Customer {} has no civil score available", request.getCustomerId());
            return createEligibilityResponse(request.getCustomerId(), false, 
                BigDecimal.ZERO, "Civil/Credit score not available. Please generate a civil score first.", null, null);
        }
        
        log.info("Customer {} civil score: {} ({})", request.getCustomerId(), civilScore, customer.getCivilScoreCategory());
        
        // Automatic rejection if civil score is below minimum threshold
        if (civilScore < MINIMUM_CIVIL_SCORE) {
            log.info("Customer {} rejected due to low civil score: {} (minimum required: {})", 
                    request.getCustomerId(), civilScore, MINIMUM_CIVIL_SCORE);
            return createEligibilityResponse(request.getCustomerId(), false, 
                BigDecimal.ZERO, 
                String.format("Civil/Credit score too low (%d). Minimum required: %d. Category: %s", 
                    civilScore, MINIMUM_CIVIL_SCORE, customer.getCivilScoreCategory()), 
                null, null);
        }
        
        // Business logic for eligibility check
        BigDecimal debtToIncomeRatio = calculateDebtToIncomeRatio(
            request.getMonthlyIncome(), 
            request.getMonthlyExpenses()
        );
        
        // Adjust DTI threshold based on civil score
        BigDecimal maxDTI = getMaxDTIForCivilScore(civilScore);
        
        // Eligibility criteria (now considers civil score)
        boolean eligible = debtToIncomeRatio.compareTo(maxDTI) <= 0 &&
                          request.getRequestedLoanAmount().compareTo(new BigDecimal("500000")) <= 0;
        
        BigDecimal eligibleAmount = BigDecimal.ZERO;
        BigDecimal interestRate = null;
        Integer recommendedTerm = null;
        
        if (eligible) {
            // Calculate eligible amount based on income and civil score
            BigDecimal incomeMultiplier = getIncomeMultiplierForCivilScore(civilScore);
            eligibleAmount = request.getMonthlyIncome()
                .multiply(incomeMultiplier)
                .min(request.getRequestedLoanAmount());
            
            // Calculate interest rate based on civil score (better score = lower rate)
            interestRate = getInterestRateForCivilScore(civilScore, debtToIncomeRatio);
            
            recommendedTerm = request.getLoanTermMonths() != null ? 
                request.getLoanTermMonths() : 60;
        }
        
        String reason = buildEligibilityReason(eligible, civilScore, customer.getCivilScoreCategory(), 
                debtToIncomeRatio, maxDTI, request.getRequestedLoanAmount());
        
        EligibilityResponseDTO response = createEligibilityResponse(
            request.getCustomerId(),
            eligible,
            eligibleAmount,
            reason,
            interestRate,
            recommendedTerm
        );
        
        log.info("Eligibility check completed. Eligible: {}, Civil Score: {}, Interest Rate: {}", 
                eligible, civilScore, interestRate);
        return response;
    }
    
    /**
     * Get maximum debt-to-income ratio allowed based on civil score
     * Higher civil score = more lenient DTI threshold
     */
    private BigDecimal getMaxDTIForCivilScore(int civilScore) {
        if (civilScore >= EXCELLENT_CIVIL_SCORE) {
            return new BigDecimal("0.50"); // 50% DTI for excellent credit
        } else if (civilScore >= GOOD_CIVIL_SCORE) {
            return new BigDecimal("0.45"); // 45% DTI for good credit
        } else {
            return new BigDecimal("0.40"); // 40% DTI for fair credit
        }
    }
    
    /**
     * Get income multiplier for eligible amount calculation based on civil score
     * Higher civil score = higher loan amount eligibility
     */
    private BigDecimal getIncomeMultiplierForCivilScore(int civilScore) {
        if (civilScore >= EXCELLENT_CIVIL_SCORE) {
            return new BigDecimal("48"); // 4 years of income for excellent credit
        } else if (civilScore >= GOOD_CIVIL_SCORE) {
            return new BigDecimal("36"); // 3 years of income for good credit
        } else {
            return new BigDecimal("24"); // 2 years of income for fair credit
        }
    }
    
    /**
     * Calculate interest rate based on civil score and DTI ratio
     * Better civil score = lower interest rate
     */
    private BigDecimal getInterestRateForCivilScore(int civilScore, BigDecimal debtToIncomeRatio) {
        BigDecimal baseRate;
        
        // Base rate based on civil score
        if (civilScore >= 800) {           // Excellent (800-850)
            baseRate = new BigDecimal("4.5");
        } else if (civilScore >= 740) {    // Very Good (740-799)
            baseRate = new BigDecimal("5.0");
        } else if (civilScore >= 670) {    // Good (670-739)
            baseRate = new BigDecimal("6.0");
        } else if (civilScore >= 580) {    // Fair (580-669)
            baseRate = new BigDecimal("7.5");
        } else {
            baseRate = new BigDecimal("9.0"); // Should not reach here due to minimum check
        }
        
        // Add adjustment based on DTI ratio
        BigDecimal dtiAdjustment = BigDecimal.ZERO;
        if (debtToIncomeRatio.compareTo(new BigDecimal("0.35")) > 0) {
            dtiAdjustment = new BigDecimal("0.5");
        } else if (debtToIncomeRatio.compareTo(new BigDecimal("0.40")) > 0) {
            dtiAdjustment = new BigDecimal("1.0");
        }
        
        return baseRate.add(dtiAdjustment);
    }
    
    private String buildEligibilityReason(boolean eligible, int civilScore, String scoreCategory, 
            BigDecimal dti, BigDecimal maxDTI, BigDecimal requestedAmount) {
        if (eligible) {
            return String.format("Customer approved. Civil Score: %d (%s), DTI: %.2f%% (max allowed: %.2f%%)", 
                    civilScore, scoreCategory, dti.multiply(new BigDecimal("100")), maxDTI.multiply(new BigDecimal("100")));
        } else {
            StringBuilder reason = new StringBuilder("Not eligible: ");
            if (dti.compareTo(maxDTI) > 0) {
                reason.append(String.format("DTI ratio %.2f%% exceeds maximum %.2f%% for civil score %d. ", 
                        dti.multiply(new BigDecimal("100")), maxDTI.multiply(new BigDecimal("100")), civilScore));
            }
            if (requestedAmount.compareTo(new BigDecimal("500000")) > 0) {
                reason.append("Requested amount exceeds maximum limit of $500,000. ");
            }
            return reason.toString().trim();
        }
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
