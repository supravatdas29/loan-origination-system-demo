package los.eligibility.config;

import los.common.messaging.CustomerResponseMessage;
import los.eligibility.service.EligibilityKafkaConsumer.EligibilityRequestMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "los.communication.mode", havingValue = "ASYNC")
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    // Consumer factory for CustomerResponseMessage (used by AsyncCommunicationStrategy)
    @Bean
    public ConsumerFactory<String, CustomerResponseMessage> customerResponseConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "eligibility-service-group");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        JsonDeserializer<CustomerResponseMessage> deserializer = new JsonDeserializer<>(CustomerResponseMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), deserializer);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CustomerResponseMessage> customerResponseKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CustomerResponseMessage> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(customerResponseConsumerFactory());
        return factory;
    }
    
    // Consumer factory for EligibilityRequestMessage (used by EligibilityKafkaConsumer)
    @Bean
    public ConsumerFactory<String, EligibilityRequestMessage> eligibilityRequestConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "eligibility-service-group");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        JsonDeserializer<EligibilityRequestMessage> deserializer = new JsonDeserializer<>(EligibilityRequestMessage.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), deserializer);
    }
    
    @Bean
    @Primary
    public ConcurrentKafkaListenerContainerFactory<String, EligibilityRequestMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EligibilityRequestMessage> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eligibilityRequestConsumerFactory());
        return factory;
    }
}
