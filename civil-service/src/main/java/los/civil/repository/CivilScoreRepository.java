package los.civil.repository;

import los.civil.entity.CivilScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CivilScoreRepository extends JpaRepository<CivilScore, Long> {
    
    /**
     * Find the latest active civil score for a customer
     */
    @Query("SELECT c FROM CivilScore c WHERE c.customerId = :customerId AND c.isActive = true ORDER BY c.generatedAt DESC LIMIT 1")
    Optional<CivilScore> findLatestActiveByCustomerId(Long customerId);
    
    /**
     * Find all civil scores for a customer
     */
    List<CivilScore> findByCustomerIdOrderByGeneratedAtDesc(Long customerId);
    
    /**
     * Find all active scores for a customer
     */
    List<CivilScore> findByCustomerIdAndIsActiveTrueOrderByGeneratedAtDesc(Long customerId);
    
    /**
     * Check if customer has a valid (non-expired, active) score
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CivilScore c " +
           "WHERE c.customerId = :customerId AND c.isActive = true AND c.expiresAt > CURRENT_TIMESTAMP")
    boolean hasValidScore(Long customerId);
}
