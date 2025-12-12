# Deployment Guide

## Docker Setup

The project includes a `docker-compose.yml` file for running PostgreSQL and Kafka in Docker containers.

### Starting Infrastructure

1. **Start PostgreSQL and Kafka:**
   ```bash
   docker-compose up -d
   ```

2. **Verify containers are running:**
   ```bash
   docker-compose ps
   ```

3. **Check logs:**
   ```bash
   docker-compose logs -f
   ```

### Database Setup

The PostgreSQL container will automatically create the required databases (`customerdb` and `loanapplicationdb`) on first startup using the `init-databases.sh` script.

If you need to create databases manually:
```bash
docker exec -it los-postgres psql -U postgres
CREATE DATABASE customerdb;
CREATE DATABASE loanapplicationdb;
```

## Deploying Services

### For Client A (Synchronous Mode)

1. **Set communication mode to SYNC** in `application.yml` or via environment variable:
   ```yaml
   los:
     communication:
       mode: SYNC
   ```

2. **Start Eureka Server:**
   ```bash
   cd eureka-server
   mvn spring-boot:run
   ```

3. **Start all services with SYNC mode:**
   ```bash
   # Terminal 1
   cd customer-service
   export COMMUNICATION_MODE=SYNC
   mvn spring-boot:run

   # Terminal 2
   cd eligibility-service
   export COMMUNICATION_MODE=SYNC
   mvn spring-boot:run

   # Terminal 3
   cd loan-application-service
   export COMMUNICATION_MODE=SYNC
   mvn spring-boot:run
   ```

4. **Verify services are registered with Eureka:**
   - Open http://localhost:8761
   - You should see all three services registered

### For Client B (Asynchronous Mode)

1. **Set communication mode to ASYNC** in `application.yml` or via environment variable:
   ```yaml
   los:
     communication:
       mode: ASYNC
   ```

2. **Ensure Kafka is running:**
   ```bash
   docker-compose ps kafka
   ```

3. **Create Kafka topics (if not auto-created):**
   ```bash
   docker exec -it los-kafka kafka-topics.sh --create --topic customer-request-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
   docker exec -it los-kafka kafka-topics.sh --create --topic customer-response-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
   docker exec -it los-kafka kafka-topics.sh --create --topic eligibility-request-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
   docker exec -it los-kafka kafka-topics.sh --create --topic eligibility-response-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
   ```

4. **Start all services with ASYNC mode:**
   ```bash
   # Terminal 1
   cd customer-service
   export COMMUNICATION_MODE=ASYNC
   mvn spring-boot:run

   # Terminal 2
   cd eligibility-service
   export COMMUNICATION_MODE=ASYNC
   mvn spring-boot:run

   # Terminal 3
   cd loan-application-service
   export COMMUNICATION_MODE=ASYNC
   mvn spring-boot:run
   ```

5. **Eureka is NOT required for async mode**

## Configuration

### Environment Variables

All services support the following environment variables:

- `COMMUNICATION_MODE`: `SYNC` or `ASYNC` (default: `SYNC`)
- `DATASOURCE_URL`: PostgreSQL connection URL (default: `jdbc:postgresql://localhost:5432/customerdb` or `loanapplicationdb`)
- `DATASOURCE_USERNAME`: PostgreSQL username (default: `postgres`)
- `DATASOURCE_PASSWORD`: PostgreSQL password (default: `postgres`)
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: `localhost:9092`)
- `EUREKA_URL`: Eureka server URL (default: `http://localhost:8761/eureka`)
- `SHOW_SQL`: Enable SQL logging (default: `false`)

### Application Properties

Each service's `application.yml` can be customized:

```yaml
los:
  communication:
    mode: ${COMMUNICATION_MODE:SYNC}  # Override default mode

spring:
  datasource:
    url: ${DATASOURCE_URL:jdbc:postgresql://localhost:5432/customerdb}
    username: ${DATASOURCE_USERNAME:postgres}
    password: ${DATASOURCE_PASSWORD:postgres}

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

## Building JARs

To build deployable JAR files:

```bash
# Build all modules
mvn clean package

# Individual services
cd customer-service && mvn clean package
cd eligibility-service && mvn clean package
cd loan-application-service && mvn clean package
```

JARs will be in `target/` directory of each service.

## Running as JARs

```bash
# Set communication mode
export COMMUNICATION_MODE=SYNC  # or ASYNC

# Run services
java -jar customer-service/target/customer-service-1.0.0-SNAPSHOT.jar
java -jar eligibility-service/target/eligibility-service-1.0.0-SNAPSHOT.jar
java -jar loan-application-service/target/loan-application-service-1.0.0-SNAPSHOT.jar
```

## Health Checks

- **Customer Service**: http://localhost:8081/actuator/health (if actuator is added)
- **Eligibility Service**: http://localhost:8082/actuator/health
- **Loan Application Service**: http://localhost:8083/actuator/health
- **Eureka Dashboard**: http://localhost:8761

## Troubleshooting

### Database Connection Issues
- Verify PostgreSQL is running: `docker-compose ps postgres`
- Check database exists: `docker exec -it los-postgres psql -U postgres -l`
- Verify connection string in `application.yml`

### Kafka Connection Issues
- Verify Kafka is running: `docker-compose ps kafka`
- Check Kafka logs: `docker-compose logs kafka`
- Verify topics exist: `docker exec -it los-kafka kafka-topics.sh --list --bootstrap-server localhost:9092`

### Eureka Registration Issues (Sync Mode Only)
- Verify Eureka is running: http://localhost:8761
- Check service logs for registration errors
- Verify `los.communication.mode=SYNC` in application.yml

### Communication Mode Not Working
- Verify `los.communication.mode` property is set correctly
- Check application logs for which communication strategy is loaded
- Ensure correct beans are conditionally created (check logs for "@ConditionalOnProperty")
