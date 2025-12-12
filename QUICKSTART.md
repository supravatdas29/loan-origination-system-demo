# Quick Start Guide

## Prerequisites Check
- ✅ Java 21 installed
- ✅ Maven 3.8+ installed
- ✅ For Sync Mode: Eureka Server (included)
- ✅ For Async Mode: Apache Kafka running on localhost:9092

## Option 1: Synchronous Mode (Client A) - Using Feign

### Step 1: Build the Project
```bash
mvn clean install
```

### Step 2: Start Eureka Server
```bash
cd eureka-server
mvn spring-boot:run
```
Wait for Eureka to start (check http://localhost:8761)

### Step 3: Start Services in Order
Open 4 terminal windows:

**Terminal 1 - Customer Service:**
```bash
cd customer-service
export COMMUNICATION_MODE=SYNC
mvn spring-boot:run
```

**Terminal 2 - Eligibility Service:**
```bash
cd eligibility-service
export COMMUNICATION_MODE=SYNC
mvn spring-boot:run
```

**Terminal 3 - Loan Application Service:**
```bash
cd loan-application-service
export COMMUNICATION_MODE=SYNC
mvn spring-boot:run
```

### Step 4: Test the System
```bash
# Create a customer
curl -X POST http://localhost:8081/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "123-456-7890",
    "dateOfBirth": "1990-01-01"
  }'

# Create a loan application (will trigger eligibility check)
curl -X POST http://localhost:8083/api/loan-applications \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "loanAmount": 50000,
    "loanTermMonths": 60,
    "loanPurpose": "Home Improvement"
  }'

# Check application status
curl http://localhost:8083/api/loan-applications/1
```

## Option 2: Asynchronous Mode (Client B) - Using Kafka

### Step 1: Start Kafka
```bash
# Download Kafka from https://kafka.apache.org/downloads
# Extract and navigate to Kafka directory

# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# In another terminal, start Kafka
bin/kafka-server-start.sh config/server.properties
```

### Step 2: Create Kafka Topics
```bash
bin/kafka-topics.sh --create --topic customer-request-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --topic customer-response-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --topic eligibility-request-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
bin/kafka-topics.sh --create --topic eligibility-response-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### Step 3: Build the Project
```bash
mvn clean install
```

### Step 4: Start Services (No Eureka needed)
Open 3 terminal windows:

**Terminal 1 - Customer Service:**
```bash
cd customer-service
export COMMUNICATION_MODE=ASYNC
mvn spring-boot:run
```

**Terminal 2 - Eligibility Service:**
```bash
cd eligibility-service
export COMMUNICATION_MODE=ASYNC
mvn spring-boot:run
```

**Terminal 3 - Loan Application Service:**
```bash
cd loan-application-service
export COMMUNICATION_MODE=ASYNC
mvn spring-boot:run
```

### Step 5: Test the System
Use the same curl commands as in Option 1. The communication will happen via Kafka behind the scenes.

## Setting Communication Mode

You can set the communication mode using environment variables:

### For Sync Mode:
```bash
export COMMUNICATION_MODE=SYNC
mvn spring-boot:run
```

### For Async Mode:
```bash
export COMMUNICATION_MODE=ASYNC
mvn spring-boot:run
```

## Service URLs

- **Eureka Server**: http://localhost:8761
- **Customer Service**: http://localhost:8081
- **Eligibility Service**: http://localhost:8082
- **Loan Application Service**: http://localhost:8083

## Troubleshooting

### Eureka Not Starting
- Check if port 8761 is available
- Verify Java version is 21+

### Kafka Connection Issues
- Ensure Kafka is running: `bin/kafka-topics.sh --list --bootstrap-server localhost:9092`
- Check Kafka logs for errors
- Verify topics are created

### Service Not Registering with Eureka
- Only needed in SYNC mode
- Check Eureka dashboard: http://localhost:8761
- Verify `eureka.client.register-with-eureka=true` in application.yml

### Communication Mode Not Working
- Verify `los.communication.mode` is set correctly
- Check logs for which communication strategy is loaded
- Ensure correct beans are conditionally created
