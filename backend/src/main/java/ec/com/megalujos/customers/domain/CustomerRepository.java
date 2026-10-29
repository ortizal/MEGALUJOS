package ec.com.megalujos.customers.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query("SELECT c FROM Customer c WHERE " +
           "(CAST(:q AS STRING) IS NULL OR c.name ILIKE CONCAT('%', CAST(:q AS STRING), '%') " +
           "OR c.documentNumber ILIKE CONCAT('%', CAST(:q AS STRING), '%') " +
           "OR c.phone ILIKE CONCAT('%', CAST(:q AS STRING), '%'))")
    Page<Customer> search(@Param("q") String q, Pageable pageable);

    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);

    boolean existsByDocumentTypeAndDocumentNumberAndIdNot(String documentType, String documentNumber, UUID id);

    Optional<Customer> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);
}
