package los.customer.client;

import los.common.dto.CivilScoreDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "civil-service", fallback = CivilServiceClientFallback.class)
public interface CivilServiceClient {
    
    @PostMapping("/api/civil-scores/generate")
    CivilScoreDTO generateCivilScore(@RequestBody Map<String, Long> request);
    
    @GetMapping("/api/civil-scores/customer/{customerId}")
    CivilScoreDTO getLatestCivilScore(@PathVariable("customerId") Long customerId);
    
    @GetMapping("/api/civil-scores/customer/{customerId}/valid")
    Boolean hasValidCivilScore(@PathVariable("customerId") Long customerId);
}
