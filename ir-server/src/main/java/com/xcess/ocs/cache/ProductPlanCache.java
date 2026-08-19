package com.xcess.ocs.cache;

import com.xcess.ocs.dto.ProductPlanDTO;
import com.xcess.ocs.entity.ProductPlan;
import com.xcess.ocs.repository.ProductPlanRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductPlanCache {

    private final ProductPlanRepository productPlanRepository;

    // Cache to store all ProductPlanDTOs by ID
    private final Map<Long, ProductPlanDTO> productPlanCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void preloadCache() {
//        log.info("Preloading product plan cache from database...");

        List<ProductPlan> productPlans = productPlanRepository.findAll();
        for (ProductPlan plan : productPlans) {
            ProductPlanDTO dto = new ProductPlanDTO(
                    plan.getProductPlanId(),
                    plan.getName(),
                    plan.getDescription(),
                    plan.getPackageType().toString(), // Assuming it's an enum
                    new ArrayList<>() // Avoid loading relationships
            );
            productPlanCache.put(plan.getProductPlanId(), dto);
        }

        log.debug("Preloaded {} product plans into cache", productPlanCache.size());
        log.info(productPlanCache.size()+" "+ "ProductPlans");
    }

    public List<ProductPlanDTO> getAllProductPlans() {
        log.debug("Fetching all product plans from cache");
        return new ArrayList<>(productPlanCache.values());
    }

    public ProductPlanDTO getProductPlanById(Long id) {
        log.debug("Fetching product plan with ID: {} from cache", id);
        return productPlanCache.get(id);
    }

    public void addToCache(ProductPlanDTO dto) {
        log.debug("Adding product plan ID: {} to cache", dto.getProductPlanId());
        productPlanCache.put(dto.getProductPlanId(), dto);
    }

    public void updateCache(ProductPlanDTO dto) {
        log.debug("Updating cache for product plan ID: {}", dto.getProductPlanId());
        productPlanCache.put(dto.getProductPlanId(), dto);
    }

    public void removeFromCache(Long id) {
        log.debug("Removing product plan ID: {} from cache", id);
        productPlanCache.remove(id);
    }

    @Scheduled(fixedDelayString = "${cache.refresh.interval:600000}")
    public void refreshCache() {
        log.info("Refreshing product plan cache...");
        productPlanCache.clear();
        preloadCache();
    }
}
