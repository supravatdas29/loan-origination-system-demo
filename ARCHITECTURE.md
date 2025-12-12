# Architecture Documentation

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Loan Origination System                  │
└─────────────────────────────────────────────────────────────┘

┌─────────────────┐
│  Eureka Server  │ (Only for SYNC mode)
│   Port: 8761    │
└─────────────────┘
         ▲
         │ Service Discovery
         │
    ┌────┴────┬──────────────────┬────────────────────┐
    │         │                  │                    │
┌───▼───┐ ┌──▼──────────┐  ┌───▼──────────┐  ┌──────▼────────┐
│Customer│ │ Eligibility │  │Loan Application│  │   Kafka      │
│Service │ │  Service    │  │    Service     │  │(Only ASYNC)  │
│ :8081  │ │   :8082     │  │    :8083       │  │   :9092      │
└────────┘ └─────────────┘  └────────────────┘  └──────────────┘
```

## Communication Modes

### Synchronous Mode (Client A) - Feign Client

```
Loan Application Service
         │
         │ HTTP Request (Feign)
         ▼
Eligibility Service
         │
         │ HTTP Request (Feign)
         ▼
Customer Service
         │
         ▼
    Response flows back synchronously
```

**Flow:**
1. Loan Application Service receives request
2. Calls Eligibility Service via Feign (synchronous HTTP)
3. Eligibility Service calls Customer Service via Feign
4. Response flows back synchronously
5. Loan application status updated immediately

### Asynchronous Mode (Client B) - Kafka

```
Loan Application Service
         │
         │ Publishes to: eligibility-request-topic
         ▼
      Kafka Topic
         │
         ▼
Eligibility Service (Consumer)
         │
         │ Publishes to: customer-request-topic
         ▼
      Kafka Topic
         │
         ▼
Customer Service (Consumer)
         │
         │ Publishes to: customer-response-topic
         ▼
      Kafka Topic
         │
         ▼
Eligibility Service (Consumer)
         │
         │ Publishes to: eligibility-response-topic
         ▼
      Kafka Topic
         │
         ▼
Loan Application Service (Consumer)
```

**Flow:**
1. Loan Application Service receives request
2. Publishes eligibility check request to Kafka
3. Eligibility Service consumes request
4. Eligibility Service publishes customer lookup request to Kafka
5. Customer Service consumes and responds via Kafka
6. Eligibility Service processes and responds via Kafka
7. Loan Application Service consumes response and updates status

## Configuration-Based Switching

### Strategy Pattern Implementation

```java
// Common interface
public interface CommunicationStrategy {
    CustomerDTO getCustomerById(Long customerId);
    EligibilityResponseDTO checkEligibility(EligibilityRequestDTO request);
}

// Synchronous implementation
@Component
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "SYNC")
public class SyncCommunicationStrategy implements CommunicationStrategy {
    // Uses Feign clients
}

// Asynchronous implementation
@Component
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "ASYNC")
public class AsyncCommunicationStrategy implements CommunicationStrategy {
    // Uses Kafka producers/consumers
}
```

### Configuration Properties

```yaml
los:
  communication:
    mode: SYNC   # or ASYNC
```

## Services Overview

### 1. Customer Service
- **Port**: 8081
- **Database**: H2 (in-memory) or PostgreSQL
- **Responsibilities**:
  - Customer CRUD operations
  - Customer data validation
  - Customer lookup for eligibility checks

### 2. Eligibility Service
- **Port**: 8082
- **Responsibilities**:
  - Loan eligibility calculation
  - Debt-to-income ratio analysis
  - Interest rate recommendation
  - Communication Strategy: Fetches customer data via sync/async

### 3. Loan Application Service
- **Port**: 8083
- **Database**: H2 (in-memory) or PostgreSQL
- **Responsibilities**:
  - Loan application management
  - Orchestrates loan origination process
  - Triggers eligibility checks
  - Maintains application status

### 4. Eureka Server
- **Port**: 8761
- **Purpose**: Service discovery for Feign clients
- **Mode**: Only required for SYNC mode

## Kafka Topics (Async Mode)

1. **customer-request-topic**: Requests for customer data
2. **customer-response-topic**: Responses with customer data
3. **eligibility-request-topic**: Requests for eligibility checks
4. **eligibility-response-topic**: Responses with eligibility results

## Database Schema

### Customer Entity
- id (Long)
- name (String)
- email (String)
- phone (String)
- dateOfBirth (LocalDate)
- address (String)
- ssn (String)

### Loan Application Entity
- id (Long)
- customerId (Long)
- loanAmount (BigDecimal)
- loanTermMonths (Integer)
- loanPurpose (String)
- status (String)
- eligible (Boolean)
- eligibleLoanAmount (BigDecimal)
- eligibilityReason (String)
- recommendedInterestRate (BigDecimal)
- recommendedTermMonths (Integer)
- applicationDate (LocalDateTime)
- lastUpdated (LocalDateTime)

## Design Patterns Used

1. **Strategy Pattern**: Communication strategy abstraction
2. **Conditional Configuration**: `@ConditionalOnProperty` for mode-based beans
3. **Dependency Injection**: Spring's DI for loose coupling
4. **Repository Pattern**: JPA repositories for data access
5. **DTO Pattern**: Data transfer objects for API communication

## Benefits of This Architecture

### Synchronous Mode (Feign)
- ✅ Simple request-response flow
- ✅ Immediate feedback
- ✅ Easier debugging
- ✅ Lower latency for small systems
- ❌ Tight coupling
- ❌ Blocking operations
- ❌ Requires service discovery

### Asynchronous Mode (Kafka)
- ✅ Loose coupling
- ✅ Better scalability
- ✅ Fault tolerance
- ✅ Event-driven architecture
- ✅ No service discovery needed
- ❌ More complex setup
- ❌ Eventual consistency
- ❌ Requires message broker

## Scaling Considerations

### Horizontal Scaling
- Services can be scaled independently
- Kafka allows multiple consumer instances
- Eureka handles service instances in sync mode

### Load Balancing
- Eureka provides client-side load balancing in sync mode
- Kafka partitions enable parallel processing in async mode

## Security Considerations

- Add authentication/authorization (OAuth2, JWT)
- Encrypt sensitive data (SSN, financial info)
- Secure inter-service communication (HTTPS/TLS)
- Implement API rate limiting
- Add audit logging

## Monitoring and Observability

Recommended additions:
- Spring Cloud Sleuth for distributed tracing
- Micrometer for metrics
- ELK Stack for centralized logging
- Prometheus + Grafana for monitoring
