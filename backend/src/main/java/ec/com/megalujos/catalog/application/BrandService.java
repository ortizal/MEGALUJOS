package ec.com.megalujos.catalog.application;

import ec.com.megalujos.catalog.domain.Brand;
import ec.com.megalujos.catalog.domain.BrandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public List<Brand> getActiveBrands() {
        return brandRepository.findByActiveTrueOrderByNameAsc();
    }
}
