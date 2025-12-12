package los.eligibility.controller;

import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import los.eligibility.service.EligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eligibility")
@RequiredArgsConstructor
public class EligibilityController {
    
    private final EligibilityService eligibilityService;
    
    @PostMapping("/check")
    public ResponseEntity<EligibilityResponseDTO> checkEligibility(@RequestBody EligibilityRequestDTO request) {
        EligibilityResponseDTO response = eligibilityService.checkEligibility(request);
        return ResponseEntity.ok(response);
    }
}
