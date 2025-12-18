package los.civil.service;

import los.civil.dto.CivilScoreDTO;
import los.civil.dto.CivilScoreRequest;
import los.civil.entity.CivilScore;
import los.civil.repository.CivilScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CivilScoreService {
    
    private final CivilScoreRepository civilScoreRepository;
    private final Random random = new Random();
    
    @Value("${civil.score.min:300}")
    private int minScore;
    
    @Value("${civil.score.max:850}")
    private int maxScore;
    
    /**
     * Generate a new civil score for a customer
     */
    @Transactional
    public CivilScoreDTO generateScore(CivilScoreRequest request) {
        log.info("Generating civil score for customer: {}", request.getCustomerId());
        
        // Deactivate any existing active scores for this customer
        deactivateExistingScores(request.getCustomerId());
        
        // Generate random score with some weighted factors
        int score = generateRandomScore(request);
        
        // Create and save the civil score
        CivilScore civilScore = new CivilScore();
        civilScore.setCustomerId(request.getCustomerId());
        civilScore.setScore(score);
        civilScore.setCategory(CivilScore.ScoreCategory.fromScore(score));
        civilScore.setDescription(generateScoreDescription(score, civilScore.getCategory()));
        civilScore.setGeneratedAt(LocalDateTime.now());
        civilScore.setExpiresAt(LocalDateTime.now().plusDays(30));
        civilScore.setIsActive(true);
        
        CivilScore saved = civilScoreRepository.save(civilScore);
        log.info("Generated civil score {} ({}) for customer {}", score, civilScore.getCategory(), request.getCustomerId());
        
        return convertToDTO(saved);
    }
    
    /**
     * Get the latest active civil score for a customer
     */
    public Optional<CivilScoreDTO> getLatestScore(Long customerId) {
        log.info("Fetching latest civil score for customer: {}", customerId);
        return civilScoreRepository.findLatestActiveByCustomerId(customerId)
                .map(this::convertToDTO);
    }
    
    /**
     * Get civil score by ID
     */
    public Optional<CivilScoreDTO> getScoreById(Long scoreId) {
        return civilScoreRepository.findById(scoreId)
                .map(this::convertToDTO);
    }
    
    /**
     * Get all scores for a customer (history)
     */
    public List<CivilScoreDTO> getScoreHistory(Long customerId) {
        return civilScoreRepository.findByCustomerIdOrderByGeneratedAtDesc(customerId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if customer has a valid (non-expired) score
     */
    public boolean hasValidScore(Long customerId) {
        return civilScoreRepository.hasValidScore(customerId);
    }
    
    /**
     * Generate a random score with some weighted factors
     */
    private int generateRandomScore(CivilScoreRequest request) {
        // Base random score
        int baseScore = minScore + random.nextInt(maxScore - minScore + 1);
        
        // Apply some factors if provided
        int adjustment = 0;
        
        // If SSN ends with even number, slight positive bias (simulation)
        if (request.getSsn() != null && !request.getSsn().isEmpty()) {
            char lastChar = request.getSsn().charAt(request.getSsn().length() - 1);
            if (Character.isDigit(lastChar) && (lastChar - '0') % 2 == 0) {
                adjustment += random.nextInt(50);
            }
        }
        
        // Customer ID based seed for consistent-ish results (optional)
        if (request.getCustomerId() != null) {
            Random customerRandom = new Random(request.getCustomerId() * 31);
            adjustment += customerRandom.nextInt(100) - 50; // -50 to +50
        }
        
        // Clamp the score to valid range
        int finalScore = Math.max(minScore, Math.min(maxScore, baseScore + adjustment));
        
        return finalScore;
    }
    
    /**
     * Generate a description based on score
     */
    private String generateScoreDescription(int score, CivilScore.ScoreCategory category) {
        StringBuilder description = new StringBuilder();
        description.append("Civil Score: ").append(score).append(". ");
        description.append(category.getDescription()).append(". ");
        
        switch (category) {
            case EXCELLENT:
                description.append("Qualifies for best interest rates and loan terms.");
                break;
            case VERY_GOOD:
                description.append("Qualifies for competitive interest rates.");
                break;
            case GOOD:
                description.append("Qualifies for standard loan products.");
                break;
            case FAIR:
                description.append("May qualify with higher interest rates or additional requirements.");
                break;
            case POOR:
                description.append("May face difficulty qualifying for loans. Consider credit improvement.");
                break;
        }
        
        return description.toString();
    }
    
    /**
     * Deactivate existing scores for a customer
     */
    @Transactional
    private void deactivateExistingScores(Long customerId) {
        List<CivilScore> activeScores = civilScoreRepository
                .findByCustomerIdAndIsActiveTrueOrderByGeneratedAtDesc(customerId);
        
        for (CivilScore score : activeScores) {
            score.setIsActive(false);
        }
        
        if (!activeScores.isEmpty()) {
            civilScoreRepository.saveAll(activeScores);
            log.info("Deactivated {} existing scores for customer {}", activeScores.size(), customerId);
        }
    }
    
    /**
     * Convert entity to DTO
     */
    private CivilScoreDTO convertToDTO(CivilScore entity) {
        CivilScoreDTO dto = new CivilScoreDTO();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setScore(entity.getScore());
        dto.setCategory(entity.getCategory().name());
        dto.setCategoryDescription(entity.getCategory().getDescription());
        dto.setDescription(entity.getDescription());
        dto.setGeneratedAt(entity.getGeneratedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setIsActive(entity.getIsActive());
        dto.setIsExpired(entity.getExpiresAt().isBefore(LocalDateTime.now()));
        return dto;
    }
}
