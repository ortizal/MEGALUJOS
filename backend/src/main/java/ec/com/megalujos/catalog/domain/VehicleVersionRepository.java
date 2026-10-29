package ec.com.megalujos.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleVersionRepository extends JpaRepository<VehicleVersion, UUID> {
    List<VehicleVersion> findByModelIdOrderByNameAsc(UUID modelId);
    List<VehicleVersion> findAllByOrderByNameAsc();
}
