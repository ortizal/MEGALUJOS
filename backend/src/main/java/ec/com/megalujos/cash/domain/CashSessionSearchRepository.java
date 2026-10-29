package ec.com.megalujos.cash.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Consulta paginada de sesiones de caja sin modificar el repositorio original. */
@Repository
public interface CashSessionSearchRepository extends JpaRepository<CashSession, UUID> {

    @Query("""
            SELECT cs FROM CashSession cs
            WHERE (CAST(:status AS STRING) IS NULL OR cs.status = :status)
            """)
    Page<CashSession> search(@Param("status") String status, Pageable pageable);
}
