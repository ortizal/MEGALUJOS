package ec.com.megalujos.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVehicleCompatibilityRepository extends JpaRepository<ProductVehicleCompatibility, ProductVehicleCompatibilityId> {

    List<ProductVehicleCompatibility> findByIdVariantId(UUID variantId);

    void deleteByIdVariantId(UUID variantId);
}
