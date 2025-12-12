package los.loanapplication.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Conditionally enables Eureka client annotation only when communication mode is SYNC
 */
@Configuration
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "SYNC", matchIfMissing = true)
@EnableFeignClients
public class EurekaConditionalConfig {
    // Eureka client will be enabled when this configuration is active (SYNC mode)
}
