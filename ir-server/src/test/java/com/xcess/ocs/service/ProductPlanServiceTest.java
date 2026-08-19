//package com.xcess.ocs.service;
//
//import com.xcess.ocs.dto.PageResponseDTO;
//import com.xcess.ocs.dto.ProductPlanDTO;
//import com.xcess.ocs.dto.ProductPlanAssociationDTO;
//import com.xcess.ocs.entity.ProductPlan;
//import com.xcess.ocs.entity.RatePackageGroup;
//import com.xcess.ocs.exception.DuplicateNameException;
//import com.xcess.ocs.exception.ResourceNotFoundException;
//import com.xcess.ocs.repository.AccountRepository;
//import com.xcess.ocs.repository.ProductPlanAssociationRepository;
//import com.xcess.ocs.repository.ProductPlanRepository;
//import com.xcess.ocs.repository.RatePackageGroupRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class ProductPlanServiceTest {
//
//    @Mock
//    private ProductPlanRepository productPlanRepository;
//
//    @Mock
//    private ProductPlanAssociationRepository productPlanAssociationRepository;
//
//    @Mock
//    private RatePackageGroupRepository ratePackageGroupRepository;
//
//    @Mock
//    private AccountRepository accountRepository;
//
//    @InjectMocks
//    private ProductPlanService productPlanService;
//
//    private ProductPlanDTO createSampleDTO() {
//        return ProductPlanDTO.builder()
//                .name("Test Plan")
//                .description("Test Description")
//                .packageType("SELLING")
//                .ratePackageGroups(List.of(
//                        ProductPlanAssociationDTO.builder()
//                                .ratePackageGroupId(1L)
//                                .startTime(LocalDateTime.now())
//                                .endTime(LocalDateTime.now().plusDays(30))
//                                .build()))
//                .build();
//    }
//
//    private ProductPlan createSampleEntity() {
//        return ProductPlan.builder()
//                .productPlanId(1L)
//                .name("Test Plan")
//                .description("Test Description")
//                .packageType(ProductPlan.PackageType.SELLING)
//                .ratePackageGroups(new ArrayList<>())
//                .build();
//    }
//
//    private RatePackageGroup createSampleRatePackageGroup() {
//        return RatePackageGroup.builder()
//                .ratePackageGroupId(1L)
//                .name("Test Group")
//                .description("Test Group Description")
//                .packageType(RatePackageGroup.PackageType.SELLING)
//                .ratePackageAssociations(new ArrayList<>())
//                .build();
//    }
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void createProductPlan_Success() {
//        ProductPlanDTO dto = createSampleDTO();
//        ProductPlan entity = createSampleEntity();
//        RatePackageGroup group = createSampleRatePackageGroup();
//
//        when(productPlanRepository.existsByNameAndIsDeletedFalse(dto.getName())).thenReturn(false);
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(group));
//        when(productPlanRepository.save(any(ProductPlan.class))).thenReturn(entity);
//
//        ProductPlanDTO result = productPlanService.createProductPlan(dto);
//
//        assertNotNull(result);
//        assertEquals(dto.getName(), result.getName());
//        verify(productPlanRepository).existsByNameAndIsDeletedFalse(dto.getName());
//        verify(productPlanRepository).save(any(ProductPlan.class));
//    }
//
//    @Test
//    void createProductPlan_DuplicateName() {
//        ProductPlanDTO dto = createSampleDTO();
//        when(productPlanRepository.existsByNameAndIsDeletedFalse(dto.getName())).thenReturn(true);
//
//        assertThrows(DuplicateNameException.class, () -> productPlanService.createProductPlan(dto));
//        verify(productPlanRepository).existsByNameAndIsDeletedFalse(dto.getName());
//        verify(productPlanRepository, never()).save(any(ProductPlan.class));
//    }
//
//    @Test
//    void getAllProductPlans_Success() {
//        List<ProductPlan> entities = List.of(createSampleEntity(), createSampleEntity());
//        when(productPlanRepository.findAll()).thenReturn(entities);
//
//        List<ProductPlanDTO> results = productPlanService.getAllProductPlans();
//
//        assertNotNull(results);
//        assertEquals(2, results.size());
//        verify(productPlanRepository).findAll();
//    }
//
//    @Test
//    void getProductPlanById_Success() {
//        ProductPlan entity = createSampleEntity();
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(entity));
//
//        ProductPlanDTO result = productPlanService.getProductPlanById(1L);
//
//        assertNotNull(result);
//        assertEquals(entity.getName(), result.getName());
//        verify(productPlanRepository).findById(1L);
//    }
//
//    @Test
//    void getProductPlanById_NotFound() {
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> productPlanService.getProductPlanById(1L));
//        verify(productPlanRepository).findById(1L);
//    }
//
//    @Test
//    void updateProductPlan_Success() {
//        ProductPlanDTO dto = createSampleDTO();
//        ProductPlan existingEntity = createSampleEntity();
//        RatePackageGroup group = createSampleRatePackageGroup();
//
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
//        when(productPlanRepository.existsByNameAndIsDeletedFalse(dto.getName())).thenReturn(false);
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(group));
//        when(productPlanRepository.save(any(ProductPlan.class))).thenReturn(existingEntity);
//
//        ProductPlanDTO result = productPlanService.updateProductPlan(1L, dto);
//
//        assertNotNull(result);
//        assertEquals(dto.getName(), result.getName());
//        verify(productPlanRepository).findById(1L);
//        verify(productPlanRepository).save(any(ProductPlan.class));
//    }
//
//    @Test
//    void updateProductPlan_NotFound() {
//        ProductPlanDTO dto = createSampleDTO();
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> productPlanService.updateProductPlan(1L, dto));
//        verify(productPlanRepository).findById(1L);
//        verify(productPlanRepository, never()).save(any(ProductPlan.class));
//    }
//
//    @Test
//    void deleteProductPlan_Success() {
//        Long productPlanId = 1L;
//
//        // Mock repository responses
//        when(productPlanRepository.existsById(productPlanId)).thenReturn(true);
//        when(accountRepository.existsByProductPlan_ProductPlanIdAndIsDeletedFalse(productPlanId))
//                .thenReturn(false);
//        when(productPlanAssociationRepository.existsByProductPlan_ProductPlanIdAndIsDeletedFalse(productPlanId))
//                .thenReturn(false);
//        doNothing().when(productPlanRepository).deleteById(productPlanId);
//
//        // Execute
//        productPlanService.deleteProductPlan(productPlanId);
//
//        // Verify ALL interactions
//        verify(productPlanRepository).existsById(productPlanId);
//        verify(accountRepository).existsByProductPlan_ProductPlanIdAndIsDeletedFalse(productPlanId);
//        verify(productPlanAssociationRepository).existsByProductPlan_ProductPlanIdAndIsDeletedFalse(productPlanId);
//        verify(productPlanRepository).deleteById(productPlanId);
//    }
//
//    @Test
//    void deleteProductPlan_NotFound() {
//        when(productPlanRepository.existsById(1L)).thenReturn(false);
//
//        assertThrows(ResourceNotFoundException.class, () -> productPlanService.deleteProductPlan(1L));
//        verify(productPlanRepository).existsById(1L);
//        verify(productPlanRepository, never()).deleteById(1L);
//    }
//
//    @Test
//    void createProductPlan_shouldSaveAndReturnProductPlanDTO_whenValidInput() {
//        ProductPlanDTO dto = new ProductPlanDTO();
//        dto.setName("Basic Plan");
//        dto.setDescription("Some description");
//        dto.setPackageType("SELLING");
//
//        ProductPlanAssociationDTO assocDTO = new ProductPlanAssociationDTO();
//        assocDTO.setRatePackageGroupId(1L);
//        assocDTO.setStartTime(LocalDateTime.now());
//        assocDTO.setEndTime(LocalDateTime.now().plusDays(10));
//        dto.setRatePackageGroups(List.of(assocDTO));
//
//        when(productPlanRepository.existsByNameAndIsDeletedFalse("Basic Plan")).thenReturn(false);
//
//        RatePackageGroup mockGroup = new RatePackageGroup();
//        mockGroup.setRatePackageGroupId(1L);
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(mockGroup));
//
//        ArgumentCaptor<ProductPlan> captor = ArgumentCaptor.forClass(ProductPlan.class);
//
//        when(productPlanRepository.save(any(ProductPlan.class))).thenAnswer(i -> {
//            ProductPlan saved = i.getArgument(0);
//            saved.setProductPlanId(100L);
//            return saved;
//        });
//
//        ProductPlanDTO result = productPlanService.createProductPlan(dto);
//
//        verify(productPlanRepository).save(captor.capture());
//        ProductPlan savedPlan = captor.getValue();
//
//        assertEquals("Basic Plan", savedPlan.getName());
//        assertEquals("Basic Plan", result.getName());
//        assertEquals(100L, result.getProductPlanId());
//    }
//
//    @Test
//    void createProductPlan_shouldThrowDuplicateNameException_whenNameExists() {
//        ProductPlanDTO dto = new ProductPlanDTO();
//        dto.setName("Duplicate");
//
//        when(productPlanRepository.existsByNameAndIsDeletedFalse("Duplicate")).thenReturn(true);
//
//        assertThrows(DuplicateNameException.class, () -> productPlanService.createProductPlan(dto));
//        verify(productPlanRepository, never()).save(any());
//    }
//
//    @Test
//    void createProductPlan_withInvalidAssociation_throwsException() {
//        ProductPlanDTO dto = createSampleDTO();
//        when(productPlanRepository.existsByNameAndIsDeletedFalse(dto.getName())).thenReturn(false);
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> productPlanService.createProductPlan(dto));
//    }
//
//    @Test
//    void updateProductPlan_withInvalidAssociation_throwsException() {
//        ProductPlanDTO dto = createSampleDTO();
//        ProductPlan existingEntity = createSampleEntity();
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> productPlanService.updateProductPlan(1L, dto));
//    }
//
//    @Test
//    void updateProductPlan_duplicateName_throwsException() {
//        ProductPlanDTO dto = createSampleDTO();
//        ProductPlan existingEntity = createSampleEntity();
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
//        when(productPlanRepository.existsByNameAndIsDeletedFalse("Other Plan")).thenReturn(true);
//        dto.setName("Other Plan");
//        // Mock the association group to exist!
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(createSampleRatePackageGroup()));
//        assertThrows(DuplicateNameException.class, () -> productPlanService.updateProductPlan(1L, dto));
//    }
//
//    @Test
//    void deleteProductPlan_referencedByAccount_throwsException() {
//        Long productPlanId = 1L;
//        when(productPlanRepository.existsById(productPlanId)).thenReturn(true);
//        when(accountRepository.existsByProductPlan_ProductPlanIdAndIsDeletedFalse(productPlanId)).thenReturn(true);
//        assertThrows(com.xcess.ocs.exception.ForeignReferenceException.class, () -> productPlanService.deleteProductPlan(productPlanId));
//    }
//
//    @Test
//    void deleteProductPlan_referencedByAssociation_throwsException() {
//        Long productPlanId = 1L;
//        when(productPlanRepository.existsById(productPlanId)).thenReturn(true);
//        when(accountRepository.existsByProductPlan_ProductPlanIdAndIsDeletedFalse(productPlanId)).thenReturn(false);
//        when(productPlanAssociationRepository.existsByProductPlan_ProductPlanIdAndIsDeletedFalse(productPlanId)).thenReturn(true);
//        assertThrows(com.xcess.ocs.exception.ForeignReferenceException.class, () -> productPlanService.deleteProductPlan(productPlanId));
//    }
//
//    @Test
//    void createProductPlan_nullDto_throwsException() {
//        assertThrows(NullPointerException.class, () -> productPlanService.createProductPlan(null));
//    }
//
//    @Test
//    void updateProductPlan_nullDto_throwsException() {
//        ProductPlan existingEntity = createSampleEntity();
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
//        assertThrows(NullPointerException.class, () -> productPlanService.updateProductPlan(1L, null));
//    }
//
//    @Test
//    void getAllProductPlans_emptyList() {
//        when(productPlanRepository.findAll()).thenReturn(Collections.emptyList());
//        List<ProductPlanDTO> result = productPlanService.getAllProductPlans();
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void createProductPlan_emptyName_throwsException() {
//        ProductPlanDTO dto = createSampleDTO();
//        dto.setName("");
//        when(productPlanRepository.existsByNameAndIsDeletedFalse("")).thenReturn(false);
//        assertThrows(Exception.class, () -> productPlanService.createProductPlan(dto));
//    }
//
//    @Test
//    void updateProductPlan_emptyAssociations_succeeds() {
//        ProductPlanDTO dto = createSampleDTO();
//        dto.setRatePackageGroups(Collections.emptyList());
//        ProductPlan existingEntity = createSampleEntity();
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
//        when(productPlanRepository.existsByNameAndIsDeletedFalse(dto.getName())).thenReturn(false);
//        when(productPlanRepository.save(any(ProductPlan.class))).thenReturn(existingEntity);
//        ProductPlanDTO result = productPlanService.updateProductPlan(1L, dto);
//        assertNotNull(result);
//        assertEquals(dto.getName(), result.getName());
//    }
//
//    @Test
//    void createProductPlan_noAssociations_succeeds() {
//        ProductPlanDTO dto = createSampleDTO();
//        dto.setRatePackageGroups(Collections.emptyList());
//        when(productPlanRepository.existsByNameAndIsDeletedFalse(dto.getName())).thenReturn(false);
//        when(productPlanRepository.save(any(ProductPlan.class))).thenAnswer(i -> {
//            ProductPlan saved = i.getArgument(0);
//            saved.setProductPlanId(101L);
//            return saved;
//        });
//        ProductPlanDTO result = productPlanService.createProductPlan(dto);
//        assertNotNull(result);
//        assertEquals(dto.getName(), result.getName());
//        assertEquals(101L, result.getProductPlanId());
//    }
//
//    @Test
//    void updateProductPlan_duplicateName_sameAsExisting_doesNotThrow() {
//        ProductPlanDTO dto = createSampleDTO();
//        ProductPlan existingEntity = createSampleEntity();
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(existingEntity));
//        when(productPlanRepository.save(any(ProductPlan.class))).thenReturn(existingEntity);
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(createSampleRatePackageGroup()));
//        ProductPlanDTO result = productPlanService.updateProductPlan(1L, dto);
//        assertNotNull(result);
//        assertEquals(dto.getName(), result.getName());
//    }
//
//    @Test
//    void getProductPlansInPages_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 2);
//        ProductPlan plan1 = createSampleEntity();
//
//        ProductPlan plan2 = createSampleEntity();
//        plan2.setProductPlanId(2L);
//        plan2.setName("Second Plan");
//
//        Page<ProductPlan> planPage = new PageImpl<>(
//            List.of(plan1, plan2),
//            pageable,
//            4 // total elements
//        );
//
//        when(productPlanRepository.findAll(pageable)).thenReturn(planPage);
//
//        // Act
//        PageResponseDTO<ProductPlanDTO> result = productPlanService.getProductPlansInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(4, result.getPageDetails().getTotalRecords());
//        assertEquals(2, result.getPageDetails().getTotalPages());
//        assertEquals(2, result.getContent().size());
//        assertEquals("Test Plan", result.getContent().get(0).getName());
//        assertEquals("Second Plan", result.getContent().get(1).getName());
//
//        verify(productPlanRepository).findAll(pageable);
//    }
//
//    @Test
//    void getProductPlansInPages_EmptyPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<ProductPlan> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//
//        when(productPlanRepository.findAll(pageable)).thenReturn(emptyPage);
//
//        // Act
//        PageResponseDTO<ProductPlanDTO> result = productPlanService.getProductPlansInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(productPlanRepository).findAll(pageable);
//    }
//
//    @Test
//    void searchProductPlans_WithSearchTermAndPackageType() {
//        // Arrange
//        String searchTerm = "Test";
//        String packageType = "SELLING";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        ProductPlan plan = createSampleEntity();
//        Page<ProductPlan> searchResults = new PageImpl<>(
//            List.of(plan),
//            pageable,
//            1
//        );
//
//        when(productPlanRepository.searchProductPlans(searchTerm, ProductPlan.PackageType.SELLING, pageable))
//            .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<ProductPlanDTO> result = productPlanService.searchProductPlans(searchTerm, packageType, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(1, result.getContent().size());
//        assertEquals("Test Plan", result.getContent().get(0).getName());
//        assertEquals("SELLING", result.getContent().get(0).getPackageType());
//
//        verify(productPlanRepository).searchProductPlans(searchTerm, ProductPlan.PackageType.SELLING, pageable);
//    }
//
//    @Test
//    void searchProductPlans_OnlySearchTerm() {
//        // Arrange
//        String searchTerm = "Test";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        ProductPlan plan = createSampleEntity();
//        Page<ProductPlan> searchResults = new PageImpl<>(
//            List.of(plan),
//            pageable,
//            1
//        );
//
//        when(productPlanRepository.searchProductPlans(searchTerm, null, pageable))
//            .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<ProductPlanDTO> result = productPlanService.searchProductPlans(searchTerm, null, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals("Test Plan", result.getContent().get(0).getName());
//
//        verify(productPlanRepository).searchProductPlans(searchTerm, null, pageable);
//    }
//
//    @Test
//    void searchProductPlans_OnlyPackageType() {
//        // Arrange
//        String packageType = "BUYING";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        ProductPlan plan = createSampleEntity();
//        plan.setPackageType(ProductPlan.PackageType.BUYING);
//        Page<ProductPlan> searchResults = new PageImpl<>(
//            List.of(plan),
//            pageable,
//            1
//        );
//
//        when(productPlanRepository.searchProductPlans(null, ProductPlan.PackageType.BUYING, pageable))
//            .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<ProductPlanDTO> result = productPlanService.searchProductPlans(null, packageType, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals("BUYING", result.getContent().get(0).getPackageType());
//
//        verify(productPlanRepository).searchProductPlans(null, ProductPlan.PackageType.BUYING, pageable);
//    }
//
//    @Test
//    void searchProductPlans_NoResults() {
//        // Arrange
//        String searchTerm = "NonExistent";
//        String packageType = "SELLING";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Page<ProductPlan> emptyResults = new PageImpl<>(List.of(), pageable, 0);
//
//        when(productPlanRepository.searchProductPlans(searchTerm, ProductPlan.PackageType.SELLING, pageable))
//            .thenReturn(emptyResults);
//
//        // Act
//        PageResponseDTO<ProductPlanDTO> result = productPlanService.searchProductPlans(searchTerm, packageType, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(productPlanRepository).searchProductPlans(searchTerm, ProductPlan.PackageType.SELLING, pageable);
//    }
//
//    @Test
//    void searchProductPlans_InvalidPackageType() {
//        // Arrange
//        String searchTerm = "Test";
//        String invalidPackageType = "INVALID";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Act & Assert
//        assertThrows(IllegalArgumentException.class,
//            () -> productPlanService.searchProductPlans(searchTerm, invalidPackageType, pageable));
//
//        verify(productPlanRepository, never()).searchProductPlans(any(), any(), any());
//    }
//}
