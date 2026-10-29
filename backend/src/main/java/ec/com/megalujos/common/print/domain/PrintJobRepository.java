package ec.com.megalujos.common.print.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PrintJobRepository extends JpaRepository<PrintJob, UUID> {
    Page<PrintJob> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<PrintJob> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
