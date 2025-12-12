package los.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationDTO {
    private Long id;
    private Long customerId;
    private BigDecimal loanAmount;
    private Integer loanTermMonths;
    private String loanPurpose;
    private String status; // PENDING, ELIGIBILITY_CHECK, APPROVED, REJECTED
    private EligibilityResponseDTO eligibilityResponse;
    private LocalDateTime applicationDate;
    private LocalDateTime lastUpdated;
}
