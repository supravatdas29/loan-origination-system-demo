package los.common.messaging;

import los.common.dto.CustomerDTO;

public class CustomerResponseMessage {
    private String correlationId;
    private CustomerDTO customer;

    public CustomerResponseMessage() {}

    public CustomerResponseMessage(String correlationId, CustomerDTO customer) {
        this.correlationId = correlationId;
        this.customer = customer;
    }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public CustomerDTO getCustomer() { return customer; }
    public void setCustomer(CustomerDTO customer) { this.customer = customer; }
}
