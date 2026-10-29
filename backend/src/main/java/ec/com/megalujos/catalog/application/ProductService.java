package ec.com.megalujos.catalog.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.com.megalujos.catalog.domain.*;
import ec.com.megalujos.catalog.dto.ProductPublicDto;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.inventory.application.StockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final StockService stockService;
    private final ObjectMapper objectMapper;
    private final String whatsappNumber;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository variantRepository,
            StockService stockService,
            ObjectMapper objectMapper,
            @Value("${megalujos.business.whatsapp:593999999999}") String whatsappNumber
    ) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.stockService = stockService;
        this.objectMapper = objectMapper;
        this.whatsappNumber = whatsappNumber;
    }

    @Transactional(readOnly = true)
    public Page<ProductPublicDto> getPublishedProducts(UUID categoryId, UUID brandId, String search, Pageable pageable) {
        Page<Product> products = productRepository.findPublishedProducts(categoryId, brandId, search, pageable);
        return products.map(this::mapToPublicDto);
    }

    @Transactional(readOnly = true)
    public ProductPublicDto getProductPublicDetail(UUID id) {
        Product product = productRepository.findById(id)
                .filter(p -> p.isActive() && p.isPublished())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
        return mapToPublicDto(product);
    }

    public ProductPublicDto mapToPublicDto(Product product) {
        ProductPublicDto dto = new ProductPublicDto();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setProductType(product.getProductType());
        dto.setCategoryName(product.getCategory().getName());
        dto.setCategorySlug(product.getCategory().getSlug());

        if (product.getBrand() != null) {
            dto.setBrandName(product.getBrand().getName());
        }

        // Variantes y precio principal
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            ProductVariant primaryVariant = product.getVariants().stream()
                    .filter(ProductVariant::isActive)
                    .findFirst()
                    .orElse(product.getVariants().get(0));

            dto.setPrice(primaryVariant.getSalePrice());

            if ("SERVICE".equalsIgnoreCase(product.getProductType()) || !product.isTrackStock()) {
                dto.setAvailability("AVAILABLE");
                dto.setAvailableStock(BigDecimal.ZERO);
            } else {
                BigDecimal available = stockService.getAvailableStock(primaryVariant.getId(), null);
                dto.setAvailableStock(available);
                if (available.compareTo(primaryVariant.getMinStock()) > 0) {
                    dto.setAvailability("AVAILABLE");
                } else if (available.compareTo(BigDecimal.ZERO) > 0) {
                    dto.setAvailability("LOW_STOCK");
                } else {
                    dto.setAvailability("OUT_OF_STOCK");
                }
            }

            // Atributos
            try {
                if (primaryVariant.getAttributes() != null && !primaryVariant.getAttributes().isBlank()) {
                    Map<String, Object> attrs = objectMapper.readValue(primaryVariant.getAttributes(), new TypeReference<>() {});
                    dto.setAttributes(attrs);
                }
            } catch (Exception ignored) {
            }
        } else {
            dto.setPrice(BigDecimal.ZERO);
            dto.setAvailability("AVAILABLE");
        }

        // Imágenes
        List<ProductPublicDto.ProductImageDto> imageDtos = new ArrayList<>();
        if (product.getImages() != null) {
            for (ProductImage pi : product.getImages()) {
                imageDtos.add(new ProductPublicDto.ProductImageDto(
                        "/uploads/" + pi.getMediaFile().getStorageKey(),
                        pi.isPrimary(),
                        pi.getSortOrder()
                ));
            }
        }
        dto.setImages(imageDtos);

        // Generar enlace WhatsApp wa.me
        dto.setWhatsappUrl(buildWhatsAppUrl(product, dto.getPrice()));

        return dto;
    }

    private String buildWhatsAppUrl(Product product, BigDecimal price) {
        String message = String.format("¡Hola MEGALUJOS! Estoy interesado en el producto: %s (SKU: %s). Precio referencial: $%s. ¿Tienen disponibilidad?",
                product.getName(), product.getSku(), price != null ? price.toString() : "0.00");
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return String.format("https://wa.me/%s?text=%s", whatsappNumber, encodedMessage);
    }
}
