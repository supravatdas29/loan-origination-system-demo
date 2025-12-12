package los.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRequestDTO {
    private Long customerId;
    private BigDecimal requestedLoanAmount;
    private Integer loanTermMonths;
    private String loanPurpose;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
}
