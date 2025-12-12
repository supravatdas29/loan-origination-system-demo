package los.loanapplication.controller;

import jakarta.validation.Valid;
import los.common.dto.LoanApplicationDTO;
import los.loanapplication.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {
    
    private final LoanApplicationService loanApplicationService;
    
    @PostMapping
    public ResponseEntity<LoanApplicationDTO> createLoanApplication(@Valid @RequestBody LoanApplicationDTO loanApplicationDTO) {
        LoanApplicationDTO created = loanApplicationService.createLoanApplication(loanApplicationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationDTO> getLoanApplicationById(@PathVariable Long id) {
        LoanApplicationDTO application = loanApplicationService.getLoanApplicationById(id);
        return ResponseEntity.ok(application);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<LoanApplicationDTO>> getLoanApplicationsByCustomerId(@PathVariable Long customerId) {
        List<LoanApplicationDTO> applications = loanApplicationService.getLoanApplicationsByCustomerId(customerId);
        return ResponseEntity.ok(applications);
    }
    
    @GetMapping
    public ResponseEntity<List<LoanApplicationDTO>> getAllLoanApplications() {
        List<LoanApplicationDTO> applications = loanApplicationService.getAllLoanApplications();
        return ResponseEntity.ok(applications);
    }
}
