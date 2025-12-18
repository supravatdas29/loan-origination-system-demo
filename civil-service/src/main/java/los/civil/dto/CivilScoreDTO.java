package los.civil.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
