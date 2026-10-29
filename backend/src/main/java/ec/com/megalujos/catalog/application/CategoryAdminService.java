package ec.com.megalujos.catalog.application;

import ec.com.megalujos.catalog.domain.Category;
import ec.com.megalujos.catalog.domain.CategoryRepository;
import ec.com.megalujos.catalog.dto.CategoryRequest;
import ec.com.megalujos.catalog.dto.CategoryResponse;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    public CategoryAdminService(CategoryRepository categoryRepository, AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
    }

    /** Árbol completo (incluye inactivas); la paginación aplica sobre las categorías raíz. */
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategoryTree(PageRequest pageRequest) {
        List<Category> all = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"));

        Map<UUID, List<Category>> childrenByParent = new LinkedHashMap<>();
        List<Category> roots = new ArrayList<>();
        for (Category category : all) {
            if (category.getParent() == null) {
                roots.add(category);
            } else {
                childrenByParent.computeIfAbsent(category.getParent().getId(), key -> new ArrayList<>()).add(category);
            }
        }

        int total = roots.size();
        int start = pageRequest.getPageNumber() * pageRequest.getPageSize();
        int end = Math.min(start + pageRequest.getPageSize(), total);
        List<Category> pageRoots = start >= total ? List.of() : roots.subList(start, end);

        List<CategoryResponse> items = pageRoots.stream()
                .map(root -> toNode(root, childrenByParent, 0))
                .toList();
        int totalPages = pageRequest.getPageSize() == 0 ? 0 : (total + pageRequest.getPageSize() - 1) / pageRequest.getPageSize();
        return new PageResponse<>(items, pageRequest.getPageNumber(), pageRequest.getPageSize(), total, totalPages);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request, UUID actorId, String ipAddress) {
        String name = request.getName().trim();
        Category category = new Category();
        category.setName(name);
        category.setSlug(resolveSlug(request.getSlug(), name, null));
        category.setParent(findCategory(request.getParentId()));
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder());
        category.setActive(request.getActive() == null || request.getActive());

        Category saved = categoryRepository.save(category);
        auditService.record(actorId, "CATEGORY_CREATED", "Category", saved.getId(),
                Map.of("name", saved.getName(), "slug", saved.getSlug()), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request, UUID actorId, String ipAddress) {
        Category category = findCategory(id);

        if (request.getParentId() != null) {
            validateNoCycle(id, request.getParentId());
            category.setParent(findCategory(request.getParentId()));
        } else {
            category.setParent(null);
        }

        String name = request.getName().trim();
        category.setName(name);
        category.setSlug(resolveSlug(request.getSlug(), name, id));
        category.setDescription(request.getDescription());
        category.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category saved = categoryRepository.save(category);
        auditService.record(actorId, "CATEGORY_UPDATED", "Category", saved.getId(),
                Map.of("name", saved.getName(), "slug", saved.getSlug()), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse setActive(UUID id, boolean active, UUID actorId, String ipAddress) {
        Category category = findCategory(id);
        category.setActive(active);
        Category saved = categoryRepository.save(category);
        auditService.record(actorId, "CATEGORY_UPDATED", "Category", saved.getId(),
                Map.of("active", active), ipAddress);
        return toResponse(saved);
    }

    private Category findCategory(UUID id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
    }

    private void validateNoCycle(UUID id, UUID parentId) {
        if (id.equals(parentId)) {
            throw new BusinessRuleException("CATEGORY_CYCLE", "La categoría no puede ser su propia categoría padre");
        }
        Category cursor = findCategory(parentId);
        int guard = 0;
        while (cursor != null && guard++ < 100) {
            if (id.equals(cursor.getId())) {
                throw new BusinessRuleException("CATEGORY_CYCLE",
                        "La categoría padre forma un ciclo con la categoría actual");
            }
            cursor = cursor.getParent();
        }
    }

    private String resolveSlug(String provided, String name, UUID currentId) {
        String candidate = (provided != null && !provided.isBlank()) ? slugify(provided) : slugify(name);
        if (candidate.isBlank()) {
            throw new BusinessRuleException("CATEGORY_SLUG_INVALID",
                    "No se pudo generar un slug válido a partir del nombre");
        }
        String unique = candidate;
        int suffix = 2;
        while (isSlugTaken(unique, currentId)) {
            unique = candidate + "-" + suffix++;
        }
        return unique;
    }

    private boolean isSlugTaken(String slug, UUID currentId) {
        return categoryRepository.findBySlug(slug)
                .filter(existing -> currentId == null || !currentId.equals(existing.getId()))
                .isPresent();
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private CategoryResponse toNode(Category category, Map<UUID, List<Category>> childrenByParent, int depth) {
        CategoryResponse dto = toResponse(category);
        if (depth < 20) {
            List<CategoryResponse> children = childrenByParent
                    .getOrDefault(category.getId(), List.of()).stream()
                    .map(child -> toNode(child, childrenByParent, depth + 1))
                    .toList();
            dto.setChildren(new ArrayList<>(children));
        }
        return dto;
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse dto = new CategoryResponse();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        dto.setDescription(category.getDescription());
        dto.setSortOrder(category.getSortOrder());
        dto.setActive(category.isActive());
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }
}
