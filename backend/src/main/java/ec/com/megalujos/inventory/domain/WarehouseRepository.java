package ec.com.megalujos.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    Optional<Warehouse> findByBranchIdAndCode(UUID branchId, String code);
    Optional<Warehouse> findFirstByIsMainTrueAndActiveTrue();
    List<Warehouse> findByActiveTrue();
}
