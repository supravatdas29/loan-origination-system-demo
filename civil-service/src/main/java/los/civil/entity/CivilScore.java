package los.civil.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "civil_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CivilScore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long customerId;
    
    @Column(nullable = false)
    private Integer score;  // Range: 300-850
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ScoreCategory category;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime generatedAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    // Score categories based on FICO-like ranges
    public enum ScoreCategory {
        POOR(300, 579, "Poor - High risk borrower"),
        FAIR(580, 669, "Fair - Below average creditworthiness"),
        GOOD(670, 739, "Good - Acceptable creditworthiness"),
        VERY_GOOD(740, 799, "Very Good - Above average creditworthiness"),
        EXCELLENT(800, 850, "Excellent - Exceptional creditworthiness");
        
        private final int minScore;
        private final int maxScore;
        private final String description;
        
        ScoreCategory(int minScore, int maxScore, String description) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.description = description;
        }
        
        public int getMinScore() { return minScore; }
        public int getMaxScore() { return maxScore; }
        public String getDescription() { return description; }
        
        public static ScoreCategory fromScore(int score) {
            for (ScoreCategory category : values()) {
                if (score >= category.minScore && score <= category.maxScore) {
                    return category;
                }
            }
            return POOR; // Default
        }
    }
    
    @PrePersist
    public void prePersist() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = generatedAt.plusDays(30); // Score valid for 30 days
        }
        if (isActive == null) {
            isActive = true;
        }
        if (category == null && score != null) {
            category = ScoreCategory.fromScore(score);
        }
    }
}
