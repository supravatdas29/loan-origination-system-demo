package los.civil.controller;

import jakarta.validation.Valid;
import los.civil.dto.CivilScoreDTO;
import los.civil.dto.CivilScoreRequest;
import los.civil.service.CivilScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/civil-scores")
@RequiredArgsConstructor
@Slf4j
public class CivilScoreController {
    
    private final CivilScoreService civilScoreService;
    
    /**
     * Generate a new civil score for a customer
     * POST /api/civil-scores/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<CivilScoreDTO> generateScore(@Valid @RequestBody CivilScoreRequest request) {
        log.info("Request to generate civil score for customer: {}", request.getCustomerId());
        CivilScoreDTO score = civilScoreService.generateScore(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(score);
    }
    
    /**
     * Get the latest active civil score for a customer
     * GET /api/civil-scores/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<CivilScoreDTO> getLatestScore(@PathVariable Long customerId) {
        log.info("Request to get latest civil score for customer: {}", customerId);
        return civilScoreService.getLatestScore(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get a civil score by ID
     * GET /api/civil-scores/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CivilScoreDTO> getScoreById(@PathVariable Long id) {
        return civilScoreService.getScoreById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get score history for a customer
     * GET /api/civil-scores/customer/{customerId}/history
     */
    @GetMapping("/customer/{customerId}/history")
    public ResponseEntity<List<CivilScoreDTO>> getScoreHistory(@PathVariable Long customerId) {
        log.info("Request to get civil score history for customer: {}", customerId);
        List<CivilScoreDTO> history = civilScoreService.getScoreHistory(customerId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Check if customer has a valid (non-expired) score
     * GET /api/civil-scores/customer/{customerId}/valid
     */
    @GetMapping("/customer/{customerId}/valid")
    public ResponseEntity<Boolean> hasValidScore(@PathVariable Long customerId) {
        boolean hasValid = civilScoreService.hasValidScore(customerId);
        return ResponseEntity.ok(hasValid);
    }
}
