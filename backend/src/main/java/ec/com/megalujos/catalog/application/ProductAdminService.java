package ec.com.megalujos.catalog.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.com.megalujos.catalog.domain.*;
import ec.com.megalujos.catalog.dto.ProductAdminRequest;
import ec.com.megalujos.catalog.dto.ProductImageResponse;
import ec.com.megalujos.catalog.dto.ProductResponse;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ProductAdminService {

    private static final Set<String> PRODUCT_TYPES = Set.of("PHYSICAL", "SERVICE");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductImageRepository productImageRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public ProductAdminService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            ProductImageRepository productImageRepository,
            MediaFileRepository mediaFileRepository,
            ObjectMapper objectMapper,
            AuditService auditService
    ) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.productImageRepository = productImageRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(
            String q,
            UUID categoryId,
            UUID brandId,
            Boolean published,
            Boolean active,
            String productType,
            Pageable pageable
    ) {
        String pattern = (q == null || q.isBlank()) ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        String type = null;
        if (productType != null && !productType.isBlank()) {
            type = productType.trim().toUpperCase(Locale.ROOT);
            if (!PRODUCT_TYPES.contains(type)) {
                throw new BusinessRuleException("PRODUCT_TYPE_INVALID", "El tipo de producto debe ser PHYSICAL o SERVICE");
            }
        }
        Page<Product> page = productRepository.findAllAdmin(pattern, categoryId, brandId, published, active, type, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        return toResponse(findProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductAdminRequest request, UUID actorId, String ipAddress) {
        String sku = request.getSku().trim();
        validateProductType(request.getProductType());
        if (productRepository.existsBySku(sku)) {
            throw new BusinessRuleException("PRODUCT_SKU_DUPLICATED", "Ya existe un producto con el SKU [" + sku + "]");
        }

        Product product = new Product();
        product.setCategory(categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", request.getCategoryId())));
        product.setBrand(findBrand(request.getBrandId()));
        applyHeader(product, request, sku);

        Set<String> seenSkus = new HashSet<>();
        if (request.getVariants() != null) {
            for (ProductAdminRequest.VariantRequest variantRequest : request.getVariants()) {
                String variantSku = requireVariantSku(variantRequest, seenSkus);
                if (variantRepository.existsBySku(variantSku)) {
                    throw duplicatedVariantSku(variantSku);
                }
                validateBarcode(variantRequest.getBarcode(), null);
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                applyVariant(variant, variantRequest, variantSku);
                product.getVariants().add(variant);
            }
        }

        Product saved = productRepository.save(product);
        auditService.record(actorId, "PRODUCT_CREATED", "Product", saved.getId(),
                Map.of("sku", saved.getSku(), "name", saved.getName()), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductAdminRequest request, UUID actorId, String ipAddress) {
        validateProductType(request.getProductType());
        Product product = findProduct(id);

        String sku = request.getSku().trim();
        if (!product.getSku().equals(sku) && productRepository.existsBySku(sku)) {
            throw new BusinessRuleException("PRODUCT_SKU_DUPLICATED", "Ya existe un producto con el SKU [" + sku + "]");
        }

        product.setCategory(categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", request.getCategoryId())));
        product.setBrand(findBrand(request.getBrandId()));
        applyHeader(product, request, sku);
        product.setUpdatedAt(OffsetDateTime.now());

        Map<String, Object> priceChanges = new LinkedHashMap<>();
        if (request.getVariants() != null) {
            priceChanges = reconcileVariants(product, request.getVariants());
        }

        Product saved = productRepository.save(product);

        auditService.record(actorId, "PRODUCT_UPDATED", "Product", saved.getId(),
                Map.of("sku", saved.getSku(), "name", saved.getName()), ipAddress);
        for (Map.Entry<String, Object> priceChange : priceChanges.entrySet()) {
            auditService.record(actorId, "PRODUCT_PRICE_CHANGED", "ProductVariant",
                    UUID.fromString(priceChange.getKey()), priceChange.getValue(), ipAddress);
        }
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse setPublished(UUID id, boolean published, UUID actorId, String ipAddress) {
        Product product = findProduct(id);
        product.setPublished(published);
        product.setUpdatedAt(OffsetDateTime.now());
        Product saved = productRepository.save(product);
        auditService.record(actorId, "PRODUCT_PUBLISHED", "Product", saved.getId(),
                Map.of("published", published), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public ProductImageResponse addImage(UUID productId, UUID mediaId, int sortOrder, boolean isPrimary) {
        Product product = findProduct(productId);
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo", mediaId));
        if (productImageRepository.existsByProductIdAndMediaFileId(productId, mediaId)) {
            throw new BusinessRuleException("PRODUCT_IMAGE_DUPLICATED", "La imagen ya está asociada al producto");
        }
        if (isPrimary) {
            productImageRepository.clearPrimary(productId);
        }

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setMediaFile(mediaFile);
        image.setSortOrder(sortOrder);
        image.setPrimary(isPrimary);
        ProductImage saved = productImageRepository.save(image);
        return toImageResponse(saved);
    }

    @Transactional
    public void removeImage(UUID productId, UUID imageId) {
        Product product = findProduct(productId);
        ProductImage image = product.getImages().stream()
                .filter(existing -> imageId.equals(existing.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Imagen de producto", imageId));
        productImageRepository.delete(image);
    }

    @Transactional
    public ProductImageResponse setPrimaryImage(UUID productId, UUID imageId) {
        Product product = findProduct(productId);
        ProductImage image = product.getImages().stream()
                .filter(existing -> imageId.equals(existing.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Imagen de producto", imageId));
        productImageRepository.clearPrimary(productId);
        image.setPrimary(true);
        ProductImage saved = productImageRepository.save(image);
        return toImageResponse(saved);
    }

    private Map<String, Object> reconcileVariants(Product product, List<ProductAdminRequest.VariantRequest> variantRequests) {
        List<ProductVariant> existing = new ArrayList<>(product.getVariants());
        Set<UUID> touchedIds = new HashSet<>();
        Set<String> seenSkus = new HashSet<>();
        Map<String, Object> priceChanges = new LinkedHashMap<>();

        for (ProductAdminRequest.VariantRequest variantRequest : variantRequests) {
            String variantSku = requireVariantSku(variantRequest, seenSkus);
            if (variantRequest.getId() != null) {
                ProductVariant variant = existing.stream()
                        .filter(candidate -> variantRequest.getId().equals(candidate.getId()))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Variante", variantRequest.getId()));
                if (!variant.getSku().equals(variantSku) && variantRepository.existsBySkuAndIdNot(variantSku, variant.getId())) {
                    throw duplicatedVariantSku(variantSku);
                }
                validateBarcode(variantRequest.getBarcode(), variant.getId());
                BigDecimal previousPrice = variant.getSalePrice();
                applyVariant(variant, variantRequest, variantSku);
                variant.setActive(true);
                if (previousPrice.compareTo(variant.getSalePrice()) != 0) {
                    priceChanges.put(variant.getId().toString(),
                            Map.of("previousPrice", previousPrice, "newPrice", variant.getSalePrice()));
                }
                touchedIds.add(variant.getId());
            } else {
                if (variantRepository.existsBySku(variantSku)) {
                    throw duplicatedVariantSku(variantSku);
                }
                validateBarcode(variantRequest.getBarcode(), null);
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                applyVariant(variant, variantRequest, variantSku);
                product.getVariants().add(variant);
            }
        }

        for (ProductVariant variant : existing) {
            if (!touchedIds.contains(variant.getId()) && variant.isActive()) {
                variant.setActive(false);
            }
        }
        return priceChanges;
    }

    private void applyHeader(Product product, ProductAdminRequest request, String sku) {
        product.setSku(sku);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        String type = request.getProductType().trim().toUpperCase(Locale.ROOT);
        product.setProductType(type);
        product.setUnit(request.getUnit() == null || request.getUnit().isBlank() ? "UNIT" : request.getUnit().trim());
        product.setTrackStock(!"SERVICE".equals(type) && request.isTrackStock());
        product.setPublished(request.isPublished());
    }

    private void applyVariant(ProductVariant variant, ProductAdminRequest.VariantRequest request, String sku) {
        variant.setSku(sku);
        variant.setBarcode(normalizeBarcode(request.getBarcode()));
        variant.setAttributes(toJson(request.getAttributes()));
        variant.setSalePrice(request.getSalePrice());
        variant.setCostPrice(request.getCostPrice());
        variant.setMinStock(request.getMinStock() == null ? BigDecimal.ZERO : request.getMinStock());
    }

    private String requireVariantSku(ProductAdminRequest.VariantRequest request, Set<String> seenSkus) {
        String sku = request.getSku() == null ? "" : request.getSku().trim();
        if (sku.isEmpty()) {
            throw new BusinessRuleException("VARIANT_SKU_REQUIRED", "El SKU de la variante es obligatorio");
        }
        if (!seenSkus.add(sku.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("VARIANT_SKU_DUPLICATED", "SKU de variante duplicado en la solicitud: [" + sku + "]");
        }
        return sku;
    }

    private BusinessRuleException duplicatedVariantSku(String sku) {
        return new BusinessRuleException("VARIANT_SKU_DUPLICATED",
                "Ya existe una variante con el SKU [" + sku + "]");
    }

    private void validateProductType(String productType) {
        if (productType == null || !PRODUCT_TYPES.contains(productType.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessRuleException("PRODUCT_TYPE_INVALID", "El tipo de producto debe ser PHYSICAL o SERVICE");
        }
    }

    private void validateBarcode(String barcode, UUID variantId) {
        String normalized = normalizeBarcode(barcode);
        if (normalized == null) {
            return;
        }
        variantRepository.findByBarcode(normalized).ifPresent(existing -> {
            if (variantId == null || !variantId.equals(existing.getId())) {
                throw new BusinessRuleException("VARIANT_BARCODE_DUPLICATED",
                        "Ya existe una variante con el código de barras [" + normalized + "]");
            }
        });
    }

    private String normalizeBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            return null;
        }
        return barcode.trim();
    }

    private Brand findBrand(UUID brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Marca", brandId));
    }

    private Product findProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }

    private String toJson(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (Exception ex) {
            throw new BusinessRuleException("PRODUCT_ATTRIBUTES_INVALID", "Los atributos de la variante no son válidos");
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

    private ProductResponse toResponse(Product product) {
        ProductResponse dto = new ProductResponse();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getName());
        dto.setCategorySlug(product.getCategory().getSlug());
        if (product.getBrand() != null) {
            dto.setBrandId(product.getBrand().getId());
            dto.setBrandName(product.getBrand().getName());
        }
        dto.setProductType(product.getProductType());
        dto.setUnit(product.getUnit());
        dto.setTrackStock(product.isTrackStock());
        dto.setPublished(product.isPublished());
        dto.setActive(product.isActive());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        dto.setVariants(product.getVariants().stream().map(variant -> {
            ProductResponse.VariantResponse variantDto = new ProductResponse.VariantResponse();
            variantDto.setId(variant.getId());
            variantDto.setSku(variant.getSku());
            variantDto.setBarcode(variant.getBarcode());
            variantDto.setAttributes(parseAttributes(variant.getAttributes()));
            variantDto.setSalePrice(variant.getSalePrice());
            variantDto.setCostPrice(variant.getCostPrice());
            variantDto.setMinStock(variant.getMinStock());
            variantDto.setActive(variant.isActive());
            return variantDto;
        }).toList());

        List<ProductImage> images = new ArrayList<>(product.getImages());
        images.sort(Comparator.comparing(ProductImage::isPrimary).reversed()
                .thenComparingInt(ProductImage::getSortOrder));
        dto.setImages(images.stream().map(this::toImageResponse).toList());

        return dto;
    }

    private ProductImageResponse toImageResponse(ProductImage image) {
        ProductImageResponse dto = new ProductImageResponse();
        dto.setId(image.getId());
        dto.setMediaId(image.getMediaFile().getId());
        dto.setUrl("/uploads/" + image.getMediaFile().getStorageKey());
        dto.setSortOrder(image.getSortOrder());
        dto.setPrimary(image.isPrimary());
        return dto;
    }
}
