package ec.com.megalujos.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, UUID> {
    List<VehicleModel> findByBrandIdOrderByNameAsc(UUID brandId);
    List<VehicleModel> findAllByOrderByNameAsc();
    boolean existsByBrandIdAndNameIgnoreCase(UUID brandId, String name);
    boolean existsByBrandIdAndNameIgnoreCaseAndIdNot(UUID brandId, String name, UUID id);
}
