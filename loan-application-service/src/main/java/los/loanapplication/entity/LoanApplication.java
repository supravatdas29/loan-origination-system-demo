package los.loanapplication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long customerId;
    
    @Column(nullable = false)
    private BigDecimal loanAmount;
    
    @Column(nullable = false)
    private Integer loanTermMonths;
    
    private String loanPurpose;
    
    @Column(nullable = false)
    private String status; // PENDING, ELIGIBILITY_CHECK, APPROVED, REJECTED
    
    private Boolean eligible;
    private BigDecimal eligibleLoanAmount;
    private String eligibilityReason;
    private BigDecimal recommendedInterestRate;
    private Integer recommendedTermMonths;
    
    @Column(nullable = false)
    private LocalDateTime applicationDate;
    
    private LocalDateTime lastUpdated;
}
