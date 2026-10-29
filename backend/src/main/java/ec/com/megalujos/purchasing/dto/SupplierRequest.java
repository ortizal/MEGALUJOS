package ec.com.megalujos.purchasing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupplierRequest {

    @Size(max = 30, message = "El RUC/cédula del proveedor no puede exceder 30 caracteres")
    private String taxId;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 200, message = "El nombre del proveedor no puede exceder 200 caracteres")
    private String name;

    @Size(max = 150, message = "El contacto no puede exceder 150 caracteres")
    private String contactName;

    @Size(max = 200, message = "El email no puede exceder 200 caracteres")
    private String email;

    @Size(max = 40, message = "El teléfono no puede exceder 40 caracteres")
    private String phone;

    private String address;

    public SupplierRequest() {
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
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
