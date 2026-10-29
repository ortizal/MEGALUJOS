package ec.com.megalujos.catalog.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.com.megalujos.catalog.domain.*;
import ec.com.megalujos.catalog.dto.*;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class VehicleCatalogService {

    private final VehicleBrandRepository vehicleBrandRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleVersionRepository vehicleVersionRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVehicleCompatibilityRepository compatibilityRepository;
    private final ObjectMapper objectMapper;

    public VehicleCatalogService(
            VehicleBrandRepository vehicleBrandRepository,
            VehicleModelRepository vehicleModelRepository,
            VehicleVersionRepository vehicleVersionRepository,
            ProductVariantRepository variantRepository,
            ProductVehicleCompatibilityRepository compatibilityRepository,
            ObjectMapper objectMapper
    ) {
        this.vehicleBrandRepository = vehicleBrandRepository;
        this.vehicleModelRepository = vehicleModelRepository;
        this.vehicleVersionRepository = vehicleVersionRepository;
        this.variantRepository = variantRepository;
        this.compatibilityRepository = compatibilityRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<VehicleBrandResponse> listBrands() {
        return vehicleBrandRepository.findAllByOrderByNameAsc().stream()
                .map(this::toBrandResponse)
                .toList();
    }

    @Transactional
    public VehicleBrandResponse createBrand(VehicleBrandRequest request) {
        String name = request.getName().trim();
        if (vehicleBrandRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("VEHICLE_BRAND_DUPLICATED", "Ya existe una marca de vehículo con el nombre [" + name + "]");
        }
        VehicleBrand saved = vehicleBrandRepository.save(new VehicleBrand(name));
        return toBrandResponse(saved);
    }

    @Transactional
    public VehicleBrandResponse updateBrand(UUID id, VehicleBrandRequest request) {
        VehicleBrand brand = vehicleBrandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca de vehículo", id));
        String name = request.getName().trim();
        if (!brand.getName().equalsIgnoreCase(name) && vehicleBrandRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("VEHICLE_BRAND_DUPLICATED", "Ya existe una marca de vehículo con el nombre [" + name + "]");
        }
        brand.setName(name);
        VehicleBrand saved = vehicleBrandRepository.save(brand);
        return toBrandResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VehicleModelResponse> listModels(UUID brandId) {
        List<VehicleModel> models = brandId == null
                ? vehicleModelRepository.findAllByOrderByNameAsc()
                : vehicleModelRepository.findByBrandIdOrderByNameAsc(brandId);
        return models.stream().map(this::toModelResponse).toList();
    }

    @Transactional
    public VehicleModelResponse createModel(VehicleModelRequest request) {
        VehicleBrand brand = vehicleBrandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Marca de vehículo", request.getBrandId()));
        String name = request.getName().trim();
        if (vehicleModelRepository.existsByBrandIdAndNameIgnoreCase(brand.getId(), name)) {
            throw new BusinessRuleException("VEHICLE_MODEL_DUPLICATED",
                    "Ya existe el modelo [" + name + "] para la marca [" + brand.getName() + "]");
        }
        VehicleModel model = new VehicleModel();
        model.setBrand(brand);
        model.setName(name);
        VehicleModel saved = vehicleModelRepository.save(model);
        return toModelResponse(saved);
    }

    @Transactional
    public VehicleModelResponse updateModel(UUID id, VehicleModelRequest request) {
        VehicleModel model = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de vehículo", id));
        VehicleBrand brand = vehicleBrandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Marca de vehículo", request.getBrandId()));
        String name = request.getName().trim();
        if (vehicleModelRepository.existsByBrandIdAndNameIgnoreCaseAndIdNot(brand.getId(), name, model.getId())) {
            throw new BusinessRuleException("VEHICLE_MODEL_DUPLICATED",
                    "Ya existe el modelo [" + name + "] para la marca [" + brand.getName() + "]");
        }
        model.setBrand(brand);
        model.setName(name);
        VehicleModel saved = vehicleModelRepository.save(model);
        return toModelResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VehicleVersionResponse> listVersions(UUID modelId) {
        List<VehicleVersion> versions = modelId == null
                ? vehicleVersionRepository.findAllByOrderByNameAsc()
                : vehicleVersionRepository.findByModelIdOrderByNameAsc(modelId);
        return versions.stream().map(this::toVersionResponse).toList();
    }

    @Transactional
    public VehicleVersionResponse createVersion(VehicleVersionRequest request) {
        VehicleModel model = vehicleModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de vehículo", request.getModelId()));
        validateYearRange(request.getYearFrom(), request.getYearTo());
        VehicleVersion version = new VehicleVersion();
        version.setModel(model);
        version.setName(request.getName().trim());
        version.setYearFrom(request.getYearFrom());
        version.setYearTo(request.getYearTo());
        VehicleVersion saved = vehicleVersionRepository.save(version);
        return toVersionResponse(saved);
    }

    @Transactional
    public VehicleVersionResponse updateVersion(UUID id, VehicleVersionRequest request) {
        VehicleVersion version = vehicleVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Versión de vehículo", id));
        VehicleModel model = vehicleModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Modelo de vehículo", request.getModelId()));
        validateYearRange(request.getYearFrom(), request.getYearTo());
        version.setModel(model);
        version.setName(request.getName().trim());
        version.setYearFrom(request.getYearFrom());
        version.setYearTo(request.getYearTo());
        VehicleVersion saved = vehicleVersionRepository.save(version);
        return toVersionResponse(saved);
    }

    /** Reemplaza por completo la lista de compatibilidades de una variante. */
    @Transactional
    public List<VehicleCompatibilityResponse> replaceCompatibility(UUID variantId, VehicleCompatibilityRequest request) {
        variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante", variantId));

        Map<UUID, VehicleVersion> versions = new LinkedHashMap<>();
        List<VehicleCompatibilityRequest.Item> items = request.getItems() == null ? List.of() : request.getItems();
        for (VehicleCompatibilityRequest.Item item : items) {
            if (!versions.containsKey(item.getVehicleVersionId())) {
                VehicleVersion version = vehicleVersionRepository.findById(item.getVehicleVersionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Versión de vehículo", item.getVehicleVersionId()));
                versions.put(version.getId(), version);
            }
        }

        compatibilityRepository.deleteByIdVariantId(variantId);

        List<ProductVehicleCompatibility> toSave = new ArrayList<>();
        Set<UUID> uniqueVersionIds = new LinkedHashSet<>();
        for (VehicleCompatibilityRequest.Item item : items) {
            if (!uniqueVersionIds.add(item.getVehicleVersionId())) {
                continue;
            }
            toSave.add(new ProductVehicleCompatibility(
                    new ProductVehicleCompatibilityId(variantId, item.getVehicleVersionId()),
                    item.isVerified(),
                    item.getNotes()
            ));
        }
        compatibilityRepository.saveAll(toSave);

        return toSave.stream().map(compatibility -> {
            VehicleVersion version = versions.get(compatibility.getId().getVehicleVersionId());
            VehicleCompatibilityResponse dto = new VehicleCompatibilityResponse();
            dto.setVehicleVersionId(version.getId());
            dto.setVehicleVersionName(version.getName());
            dto.setVerified(compatibility.isVerified());
            dto.setNotes(compatibility.getNotes());
            return dto;
        }).toList();
    }

    @Transactional(readOnly = true)
    public VariantLookupResponse lookupByBarcode(String barcode) {
        ProductVariant variant = variantRepository.findByBarcode(barcode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Variante", barcode));
        Product product = variant.getProduct();

        VariantLookupResponse dto = new VariantLookupResponse();
        dto.setVariantId(variant.getId());
        dto.setSku(variant.getSku());
        dto.setBarcode(variant.getBarcode());
        dto.setSalePrice(variant.getSalePrice());
        dto.setAttributes(parseAttributes(variant.getAttributes()));

        VariantLookupResponse.ProductSummary summary = new VariantLookupResponse.ProductSummary();
        summary.setId(product.getId());
        summary.setSku(product.getSku());
        summary.setName(product.getName());
        summary.setProductType(product.getProductType());
        summary.setUnit(product.getUnit());
        summary.setCategoryName(product.getCategory().getName());
        if (product.getBrand() != null) {
            summary.setBrandName(product.getBrand().getName());
        }
        summary.setPublished(product.isPublished());
        summary.setActive(product.isActive());
        dto.setProduct(summary);
        return dto;
    }

    private void validateYearRange(Integer yearFrom, Integer yearTo) {
        if (yearFrom != null && yearTo != null && yearTo < yearFrom) {
            throw new BusinessRuleException("VEHICLE_YEAR_RANGE", "El año final no puede ser menor al año inicial");
        }
    }

    private Map<String, Object> parseAttributes(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private VehicleBrandResponse toBrandResponse(VehicleBrand brand) {
        VehicleBrandResponse dto = new VehicleBrandResponse();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        return dto;
    }

    private VehicleModelResponse toModelResponse(VehicleModel model) {
        VehicleModelResponse dto = new VehicleModelResponse();
        dto.setId(model.getId());
        dto.setBrandId(model.getBrand().getId());
        dto.setBrandName(model.getBrand().getName());
        dto.setName(model.getName());
        return dto;
    }

    private VehicleVersionResponse toVersionResponse(VehicleVersion version) {
        VehicleVersionResponse dto = new VehicleVersionResponse();
        dto.setId(version.getId());
        dto.setModelId(version.getModel().getId());
        dto.setModelName(version.getModel().getName());
        dto.setName(version.getName());
        dto.setYearFrom(version.getYearFrom());
        dto.setYearTo(version.getYearTo());
        return dto;
    }
}
