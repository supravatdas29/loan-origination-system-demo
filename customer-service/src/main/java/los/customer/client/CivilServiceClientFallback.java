package los.customer.client;

import los.common.dto.CivilScoreDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class CivilServiceClientFallback implements CivilServiceClient {
    
    @Override
    public CivilScoreDTO generateCivilScore(Map<String, Long> request) {
        log.warn("Fallback: Unable to generate civil score for customer: {}", request.get("customerId"));
        return createFallbackDTO(request.get("customerId"));
    }
    
    @Override
    public CivilScoreDTO getLatestCivilScore(Long customerId) {
        log.warn("Fallback: Unable to get civil score for customer: {}", customerId);
        return createFallbackDTO(customerId);
    }
    
    @Override
    public Boolean hasValidCivilScore(Long customerId) {
        log.warn("Fallback: Unable to check valid civil score for customer: {}", customerId);
        return false;
    }
    
    private CivilScoreDTO createFallbackDTO(Long customerId) {
        CivilScoreDTO dto = new CivilScoreDTO();
        dto.setCustomerId(customerId);
        dto.setScore(0); // Zero indicates fallback/unknown score
        dto.setCategory("UNKNOWN");
        dto.setDescription("Civil score service unavailable");
        return dto;
    }
}
