package los.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResponseDTO {
    private Long customerId;
    private Boolean eligible;
    private BigDecimal eligibleLoanAmount;
    private String reason;
    private BigDecimal recommendedInterestRate;
    private Integer recommendedTermMonths;
}
