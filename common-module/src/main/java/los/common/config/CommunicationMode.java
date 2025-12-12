package los.common.config;

public enum CommunicationMode {
    SYNC,   // Synchronous using Feign
    ASYNC   // Asynchronous using Kafka
}
