# Loan Origination System (LOS) - Multi-Microservices Architecture

A comprehensive Loan Origination System built with Spring Boot microservices architecture supporting both **Synchronous (Feign)** and **Asynchronous (Kafka)** communication patterns.

## Architecture Overview

The system consists of the following microservices:

1. **Eureka Server** - Service discovery server (required for sync mode)
2. **Customer Service** - Manages customer information
3. **Eligibility Service** - Checks loan eligibility for customers
4. **Loan Application Service** - Manages loan applications and orchestrates the loan origination process

## Communication Modes

### Synchronous Mode (Client A)
- Uses **Feign Client** for inter-service communication
- Requires **Eureka Server** for service discovery
- Direct HTTP calls between services
- Immediate response handling

### Asynchronous Mode (Client B)
- Uses **Apache Kafka** for message-based communication
- Event-driven architecture
- Decoupled services
- Better scalability and fault tolerance

## Configuration

The communication mode can be switched using the `los.communication.mode` property:

```yaml
los:
  communication:
    mode: SYNC   # or ASYNC
```

### Setting Communication Mode

#### Option 1: Environment Variable
```bash
export COMMUNICATION_MODE=SYNC   # or ASYNC
```

#### Option 2: Application Properties
Edit `application.yml` in each service:
```yaml
los:
  communication:
    mode: SYNC   # or ASYNC
```

Or use environment variable:
```bash
export COMMUNICATION_MODE=SYNC   # or ASYNC
mvn spring-boot:run
```

## Prerequisites

### For Synchronous Mode (Client A)
- Java 21+
- Maven 3.8+
- PostgreSQL or H2 (configured in application.yml)
- Eureka Server (included)

### For Asynchronous Mode (Client B)
- Java 21+
- Maven 3.8+
- PostgreSQL or H2 (configured in application.yml)
- Apache Kafka (running on localhost:9092 by default)

### Installing Apache Kafka
```bash
# Download Kafka from https://kafka.apache.org/downloads
# Extract and start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka server
bin/kafka-server-start.sh config/server.properties
```

## Building the Project

```bash
# Build all modules
mvn clean install

# Build specific service
cd customer-service
mvn clean package
```

## Running the Services

### Client A - Synchronous Mode

#### 1. Start Eureka Server
```bash
cd eureka-server
mvn spring-boot:run
# Access at http://localhost:8761
```

#### 2. Start Customer Service
```bash
cd customer-service
export COMMUNICATION_MODE=SYNC
mvn spring-boot:run
# Runs on http://localhost:8081
```

#### 3. Start Eligibility Service
```bash
cd eligibility-service
export COMMUNICATION_MODE=SYNC
mvn spring-boot:run
# Runs on http://localhost:8082
```

#### 4. Start Loan Application Service
```bash
cd loan-application-service
export COMMUNICATION_MODE=SYNC
mvn spring-boot:run
# Runs on http://localhost:8083
```

### Client B - Asynchronous Mode

#### 1. Start Kafka (if not already running)
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka
bin/kafka-server-start.sh config/server.properties
```

#### 2. Create Kafka Topics
```bash
bin/kafka-topics.sh --create --topic customer-request-topic --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic customer-response-topic --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic eligibility-request-topic --bootstrap-server localhost:9092
bin/kafka-topics.sh --create --topic eligibility-response-topic --bootstrap-server localhost:9092
```

#### 3. Start Services (Eureka not required)
```bash
# Start Customer Service
cd customer-service
export COMMUNICATION_MODE=ASYNC
mvn spring-boot:run

# Start Eligibility Service
cd eligibility-service
export COMMUNICATION_MODE=ASYNC
mvn spring-boot:run

# Start Loan Application Service
cd loan-application-service
export COMMUNICATION_MODE=ASYNC
mvn spring-boot:run
```

## API Endpoints

### Customer Service
- `POST /api/customers` - Create customer
- `GET /api/customers/{id}` - Get customer by ID
- `GET /api/customers` - Get all customers
- `PUT /api/customers/{id}` - Update customer

### Eligibility Service
- `POST /api/eligibility/check` - Check loan eligibility

### Loan Application Service
- `POST /api/loan-applications` - Create loan application
- `GET /api/loan-applications/{id}` - Get application by ID
- `GET /api/loan-applications/customer/{customerId}` - Get applications by customer
- `GET /api/loan-applications` - Get all applications

## Example Usage

### Creating a Loan Application (Sync Mode)

```bash
# 1. Create a customer
curl -X POST http://localhost:8081/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "123-456-7890",
    "dateOfBirth": "1990-01-01"
  }'

# 2. Create a loan application
curl -X POST http://localhost:8083/api/loan-applications \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "loanAmount": 50000,
    "loanTermMonths": 60,
    "loanPurpose": "Home Improvement"
  }'

# 3. Check application status
curl http://localhost:8083/api/loan-applications/1
```

### Same Process Works for Async Mode
The same API calls work, but communication happens via Kafka behind the scenes.

## Project Structure

```
loan-origination-system/
├── pom.xml                          # Parent POM
├── common-module/                   # Shared DTOs and interfaces
├── eureka-server/                   # Service discovery server
├── customer-service/                # Customer management service
├── eligibility-service/             # Eligibility checking service
├── loan-application-service/        # Loan application service
└── config/                          # Configuration examples
    ├── client-a-sync/              # Client A (Sync) configs
    └── client-b-async/             # Client B (Async) configs
```

## Key Design Patterns

1. **Strategy Pattern** - `CommunicationStrategy` interface allows switching between sync/async
2. **Conditional Bean Configuration** - Uses `@ConditionalOnProperty` to activate beans based on mode
3. **Service Discovery** - Eureka for synchronous mode
4. **Message Broker** - Kafka for asynchronous mode

## Configuration Switching

To switch between modes for different clients:

1. **For Client A (Sync)**: Set `COMMUNICATION_MODE=SYNC` and ensure Eureka is running
2. **For Client B (Async)**: Set `COMMUNICATION_MODE=ASYNC` and ensure Kafka is running

The same codebase works for both modes - just change the configuration!

## Troubleshooting

### Eureka Connection Issues (Sync Mode)
- Ensure Eureka server is running on port 8761
- Check service registration in Eureka dashboard

### Kafka Connection Issues (Async Mode)
- Ensure Kafka is running on localhost:9092
- Verify topics are created
- Check Kafka logs for consumer/producer errors

### Communication Strategy Not Found
- Verify `los.communication.mode` is set to either `SYNC` or `ASYNC`
- Check that the correct strategy beans are conditionally created

## Future Enhancements

- Add API Gateway (Spring Cloud Gateway)
- Implement Circuit Breaker (Resilience4j)
- Add distributed tracing (Zipkin/Sleuth)
- Implement event sourcing for audit trail
- Add authentication and authorization

## License

This is a demo project for educational purposes.
