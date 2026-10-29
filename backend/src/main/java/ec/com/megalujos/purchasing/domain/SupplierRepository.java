package ec.com.megalujos.purchasing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    @Query("SELECT s FROM Supplier s WHERE " +
           "(CAST(:q AS STRING) IS NULL OR s.name ILIKE CAST(:q AS STRING) OR s.taxId ILIKE CAST(:q AS STRING))")
    Page<Supplier> search(@Param("q") String q, Pageable pageable);

    boolean existsByTaxId(String taxId);

    boolean existsByTaxIdAndIdNot(String taxId, UUID id);
}
