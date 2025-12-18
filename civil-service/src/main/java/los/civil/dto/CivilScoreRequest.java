package los.civil.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CivilScoreRequest {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    private String ssn;  // Optional: for score generation factors
    
    private String customerName;  // Optional: for logging
}
