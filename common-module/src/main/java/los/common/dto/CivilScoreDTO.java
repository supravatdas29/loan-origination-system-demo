package los.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Civil/Credit Score information
 * Used for inter-service communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CivilScoreDTO {
    private Long id;
    private Long customerId;
    private Integer score;
    private String category;
    private String categoryDescription;
    private String description;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private Boolean isExpired;
    
    /**
     * Check if the score is good enough for loan approval
     * Based on FICO-like score ranges:
     * - POOR: 300-579
     * - FAIR: 580-669
     * - GOOD: 670-739
     * - VERY_GOOD: 740-799
     * - EXCELLENT: 800-850
     */
    public boolean isEligibleForLoan() {
        return score != null && score >= 580; // FAIR and above
    }
    
    public boolean isGoodScore() {
        return score != null && score >= 670; // GOOD and above
    }
    
    public boolean isExcellentScore() {
        return score != null && score >= 740; // VERY_GOOD and above
    }
}
