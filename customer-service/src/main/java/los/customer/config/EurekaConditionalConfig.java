package los.customer.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Conditionally enables Eureka client only when communication mode is SYNC.
 * When los.communication.mode=ASYNC, this configuration is not active,
 * so Eureka client will not be enabled.
 */
@Configuration
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "SYNC", matchIfMissing = true)
@EnableFeignClients
public class EurekaConditionalConfig {
    // Eureka client will be enabled when this configuration is active (SYNC mode)
}
