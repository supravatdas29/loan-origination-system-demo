package los.common.messaging;

public class CustomerRequestMessage {
    private String correlationId;
    private Long customerId;

    public CustomerRequestMessage() {}

    public CustomerRequestMessage(String correlationId, Long customerId) {
        this.correlationId = correlationId;
        this.customerId = customerId;
    }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
}
