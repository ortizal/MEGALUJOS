package ec.com.megalujos.customers.dto;

import jakarta.validation.constraints.NotBlank;

public class CustomerRequest {

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String documentType; // CEDULA, RUC, PASAPORTE, CONSUMIDOR_FINAL

    private String documentNumber;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String email;

    private String phone;

    private String address;

    public CustomerRequest() {
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
