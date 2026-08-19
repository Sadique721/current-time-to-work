package com.xcess.ocs.controller;

import com.xcess.ocs.constants.ResponseConstants;
import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.ProductPlanSearchDTO;
import com.xcess.ocs.service.ProductPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-plans")
@RequiredArgsConstructor
@Tag(name = "8. Product Plan", description = "Endpoints for managing product plans")
@Slf4j
public class ProductPlanController {

        private final ProductPlanService productPlanService;

        @Operation(summary = "Get all product plans", description = "Returns a list of all product plans")
        @ApiResponse(responseCode = "200", description = "HTTP Status OK")
        @GetMapping
        public ResponseEntity<List<ProductPlanDTO>> getAllProductPlans() {
                log.info("REST request to get all product plans");
                List<ProductPlanDTO> plans = productPlanService.getAllProductPlans();
                log.info("Retrieved {} product plans", plans.size());
                return ResponseEntity.ok(plans);
        }

        @Operation(summary = "Get paginated and filtered list of product plans",
                description = "Returns a paginated list of product plans with search functionality")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PostMapping("/paginated")
        public ResponseEntity<PageResponseDTO<ProductPlanDTO>> getProductPlansInPage(
                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        required = true,
                        description = "Pagination request with product plan search criteria",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = PageRequestDTO.class),
                                examples = @ExampleObject(
                                        name = "ProductPlanPageRequest",
                                        value = """
                                    {
                                        "page": 1,
                                        "pageSize": 5,
                                        "searchCriteria": {
                                            "searchTerm": "Premium Plan",
                                            "packageType": "SELLING"
                                        }
                                    }
                                    """
                                )
                        )
                )
                @Valid @RequestBody PageRequestDTO<ProductPlanSearchDTO> pageRequestDTO) {
                log.info("REST request to get a filtered list of product plans in page");
                Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
                PageResponseDTO<ProductPlanDTO> response;

                if (pageRequestDTO.getSearchCriteria() != null) {
                        response = productPlanService.searchProductPlans(
                                pageRequestDTO.getSearchCriteria().getSearchTerm(),
                                pageRequestDTO.getSearchCriteria().getPackageType(),
                                pageable
                        );
                        log.info("Retrieved filtered product plans in a page");
                } else {
                        response = productPlanService.getProductPlansInPages(pageable);
                        log.info("Retrieved all product plans in a page");
                }

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get a specific product plan by ID", description = "Returns the product plan with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @GetMapping("/{id}")
        public ResponseEntity<ProductPlanDTO> getProductPlanById(@PathVariable Long id) {
                log.info("REST request to get product plan with ID: {}", id);
                ProductPlanDTO plan = productPlanService.getProductPlanById(id);
                log.info("Retrieved product plan: {}", plan.getName());
                return ResponseEntity.ok(plan);
        }

        @Operation(summary = "Create a product plan", description = "Creates a new product plan after entering ProductPlanDTO")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "409", description = "HTTP Status CONFLICT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PostMapping
        public ResponseEntity<ResponseDTO> createProductPlan(@Valid @RequestBody ProductPlanDTO productPlanDTO) {
                log.info("REST request to create product plan: {}", productPlanDTO.getName());
                productPlanService.createProductPlan(productPlanDTO);
                log.info("Product plan created successfully");
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ResponseDTO.ok(ResponseConstants.MESSAGE_201));
        }

        @Operation(summary = "Update a product plan", description = "Updates the product plan with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "409", description = "HTTP Status CONFLICT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PutMapping("/{id}")
        public ResponseEntity<ResponseDTO> updateProductPlan(
                        @PathVariable Long id,
                        @Valid @RequestBody ProductPlanDTO productPlanDTO) {
                log.info("REST request to update product plan with ID: {}", id);
                productPlanService.updateProductPlan(id, productPlanDTO);
                log.info("Product plan updated successfully");
                return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_UPDATE));
        }

        @Operation(summary = "Delete a product plan", description = "Deletes the product plan with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<ResponseDTO> deleteProductPlan(@PathVariable Long id) {
                log.info("REST request to delete product plan with ID: {}", id);
                productPlanService.deleteProductPlan(id);
                log.info("Product plan deleted successfully");
                return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_DELETE));
        }

        @Operation(summary = "Get product plans by partner type",
                   description = "Returns list of product plans based on partner type. CUSTOMER shows SELLING packages, VENDOR shows BUYING packages, BOTH shows all packages.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK")
        })
        @GetMapping("/names")
        public ResponseEntity<List<Map<String, Object>>> getProductPlansByPartnerType(
                        @RequestParam(defaultValue = "BOTH") String partnerType) {
                log.info("REST request to get product plans for partner type: {}", partnerType);
                List<Map<String, Object>> productPlans = productPlanService.getProductPlansByPartnerType(partnerType);
                log.info("Retrieved {} product plans", productPlans.size());
                return ResponseEntity.ok(productPlans);
        }
}
