package ec.com.megalujos.invoicing.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, UUID> {

    /**
     * Bloqueo pesimista de escritura sobre la fila de secuencia para garantizar
     * numeración atómica de comprobantes bajo concurrencia (SELECT FOR UPDATE).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ds FROM DocumentSequence ds WHERE ds.issuerId = :issuerId AND ds.documentType = :documentType")
    Optional<DocumentSequence> findByIssuerAndTypeForUpdate(@Param("issuerId") UUID issuerId,
                                                            @Param("documentType") String documentType);
}
