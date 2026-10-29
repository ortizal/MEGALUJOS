package ec.com.megalujos.invoicing.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Lectura del cliente desde la tabla customers (módulo customers aún no entregado
 * su entidad JPA). Consulta nativa de solo lectura; no modifica datos.
 */
@Repository
public class CustomerSnapshotReader {

    @PersistenceContext
    private EntityManager entityManager;

    public record CustomerSnapshot(String documentType, String documentNumber,
                                   String name, String email, String phone, String address) {
    }

    public CustomerSnapshot findByCustomerId(UUID customerId) {
        if (customerId == null) {
            return null;
        }
        try {
            Object row = entityManager.createNativeQuery(
                            "SELECT document_type, document_number, name, email, phone, address FROM customers WHERE id = :id")
                    .setParameter("id", customerId)
                    .getSingleResult();
            Object[] cols = (Object[]) row;
            return new CustomerSnapshot(
                    (String) cols[0],
                    (String) cols[1],
                    (String) cols[2],
                    (String) cols[3],
                    (String) cols[4],
                    (String) cols[5]
            );
        } catch (Exception ex) {
            return null;
        }
    }
}
