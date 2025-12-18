package los.customer.controller;

import jakarta.validation.Valid;
import los.common.dto.CustomerDTO;
import los.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {
    
    private final CustomerService customerService;
    
    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        CustomerDTO created = customerService.createCustomer(customerDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        CustomerDTO customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }
    
    /**
     * Get customer with civil score - fetches from civil-service if needed
     */
    @GetMapping("/{id}/with-civil-score")
    public ResponseEntity<CustomerDTO> getCustomerWithCivilScore(@PathVariable Long id) {
        log.info("Fetching customer {} with civil score", id);
        CustomerDTO customer = customerService.getCustomerWithCivilScore(id);
        return ResponseEntity.ok(customer);
    }
    
    /**
     * Refresh civil score for a customer - generates a new score
     */
    @PostMapping("/{id}/refresh-civil-score")
    public ResponseEntity<CustomerDTO> refreshCivilScore(@PathVariable Long id) {
        log.info("Refreshing civil score for customer {}", id);
        CustomerDTO customer = customerService.refreshCivilScore(id);
        return ResponseEntity.ok(customer);
    }
    
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        List<CustomerDTO> customers = customerService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable Long id, 
                                                      @Valid @RequestBody CustomerDTO customerDTO) {
        CustomerDTO updated = customerService.updateCustomer(id, customerDTO);
        return ResponseEntity.ok(updated);
    }
}
