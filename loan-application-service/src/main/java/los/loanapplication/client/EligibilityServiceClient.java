package los.loanapplication.client;

import los.common.dto.EligibilityRequestDTO;
import los.common.dto.EligibilityResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "eligibility-service")
public interface EligibilityServiceClient {
    
    @PostMapping("/api/eligibility/check")
    EligibilityResponseDTO checkEligibility(@RequestBody EligibilityRequestDTO request);
}
