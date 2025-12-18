package los.customer.service;

import los.common.dto.CivilScoreDTO;
import los.common.dto.CustomerDTO;
import los.customer.client.CivilServiceClient;
import los.customer.entity.Customer;
import los.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final CivilServiceClient civilServiceClient;
    
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        Customer customer = new Customer();
        customer.setName(customerDTO.getName());
        customer.setEmail(customerDTO.getEmail());
        customer.setPhone(customerDTO.getPhone());
        customer.setDateOfBirth(customerDTO.getDateOfBirth());
        customer.setAddress(customerDTO.getAddress());
        customer.setSsn(customerDTO.getSsn());
        
        Customer saved = customerRepository.save(customer);
        return convertToDTO(saved);
    }
    
    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        return convertToDTO(customer);
    }
    
    /**
     * Get customer with civil score - fetches from civil-service and caches in customer entity
     */
    public CustomerDTO getCustomerWithCivilScore(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        
        // Fetch or generate civil score from civil-service
        CivilScoreDTO civilScore = fetchOrGenerateCivilScore(customer.getId());
        
        // Update customer with civil score if valid
        if (civilScore != null && civilScore.getScore() > 0) {
            customer.setCivilScore(civilScore.getScore());
            customer.setCivilScoreCategory(civilScore.getCategory());
            customerRepository.save(customer);
            log.info("Updated customer {} with civil score: {} ({})", 
                    id, civilScore.getScore(), civilScore.getCategory());
        }
        
        return convertToDTO(customer);
    }
    
    /**
     * Refresh civil score for a customer - always generates a new score
     */
    @Transactional
    public CustomerDTO refreshCivilScore(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + customerId));
        
        // Generate new civil score
        Map<String, Long> request = new HashMap<>();
        request.put("customerId", customerId);
        
        CivilScoreDTO civilScore = civilServiceClient.generateCivilScore(request);
        
        if (civilScore != null && civilScore.getScore() > 0) {
            customer.setCivilScore(civilScore.getScore());
            customer.setCivilScoreCategory(civilScore.getCategory());
            customerRepository.save(customer);
            log.info("Refreshed civil score for customer {}: {} ({})", 
                    customerId, civilScore.getScore(), civilScore.getCategory());
        }
        
        return convertToDTO(customer);
    }
    
    private CivilScoreDTO fetchOrGenerateCivilScore(Long customerId) {
        try {
            // First check if customer has a valid civil score
            Boolean hasValidScore = civilServiceClient.hasValidCivilScore(customerId);
            
            if (Boolean.TRUE.equals(hasValidScore)) {
                // Get existing score
                return civilServiceClient.getLatestCivilScore(customerId);
            } else {
                // Generate new score
                Map<String, Long> request = new HashMap<>();
                request.put("customerId", customerId);
                return civilServiceClient.generateCivilScore(request);
            }
        } catch (Exception e) {
            log.error("Error fetching civil score for customer {}: {}", customerId, e.getMessage());
            return null;
        }
    }
    
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        
        customer.setName(customerDTO.getName());
        customer.setEmail(customerDTO.getEmail());
        customer.setPhone(customerDTO.getPhone());
        customer.setDateOfBirth(customerDTO.getDateOfBirth());
        customer.setAddress(customerDTO.getAddress());
        customer.setSsn(customerDTO.getSsn());
        
        Customer updated = customerRepository.save(customer);
        return convertToDTO(updated);
    }
    
    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setDateOfBirth(customer.getDateOfBirth());
        dto.setAddress(customer.getAddress());
        dto.setSsn(customer.getSsn());
        dto.setCivilScore(customer.getCivilScore());
        dto.setCivilScoreCategory(customer.getCivilScoreCategory());
        return dto;
    }
}
