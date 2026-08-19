//package com.xcess.ocs.service;
//
//import com.xcess.ocs.dto.PageResponseDTO;
//import com.xcess.ocs.dto.RatePackageAssociationDTO;
//import com.xcess.ocs.dto.RatePackageGroupDTO;
//import com.xcess.ocs.entity.RatePackage;
//import com.xcess.ocs.entity.RatePackageGroup;
//import com.xcess.ocs.exception.DuplicateNameException;
//import com.xcess.ocs.exception.ResourceNotFoundException;
//import com.xcess.ocs.repository.ProductPlanAssociationRepository;
//import com.xcess.ocs.repository.RatePackageAssociationRepository;
//import com.xcess.ocs.repository.RatePackageGroupRepository;
//import com.xcess.ocs.repository.RatePackageRepository;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class RatePackageGroupServiceTest {
//
//    @Mock
//    private RatePackageGroupRepository ratePackageGroupRepository;
//    @Mock
//    private RatePackageRepository ratePackageRepository;
//    @Mock
//    private RatePackageAssociationRepository ratePackageAssociationRepository;
//    @Mock
//    private ProductPlanAssociationRepository productPlanAssociationRepository;
//
//    @InjectMocks
//    private RatePackageGroupService service;
//    private RatePackage ratePackage;
//    private RatePackageGroup group;
//    private RatePackageGroupDTO dto;
//
//    @BeforeEach
//    void setUp() {
//        group = RatePackageGroup.builder()
//                .ratePackageGroupId(1L)
//                .name("Group A")
//                .description("Test group")
//                .packageType(RatePackageGroup.PackageType.SELLING)
//                .ratePackageAssociations(new ArrayList<>())
//                .build();
//
//        dto = new RatePackageGroupDTO();
//        dto.setRatePackageGroupName("Group A");
//        dto.setDescription("Test group");
//        dto.setPackageType("SELLING");
//        dto.setRatePackages(new ArrayList<>());
//    }
//
//    @Test
//    void createRatePackageGroup_success() {
//        when(ratePackageGroupRepository.existsByNameAndIsDeletedFalse("Group A"))
//                .thenReturn(false);
//        when(ratePackageGroupRepository.save(any(RatePackageGroup.class)))
//                .thenReturn(group);
//
//        RatePackageGroupDTO result = service.createRatePackageGroup(dto);
//
//        assertNotNull(result);
//        assertEquals("Group A", result.getRatePackageGroupName());
//        assertEquals("Test group", result.getDescription());
//        assertEquals("SELLING", result.getPackageType());
//        verify(ratePackageGroupRepository).existsByNameAndIsDeletedFalse("Group A");
//        verify(ratePackageGroupRepository).save(any(RatePackageGroup.class));
//    }
//
//    @Test
//    void createRatePackageGroup_duplicateName_throwsException() {
//        when(ratePackageGroupRepository.existsByNameAndIsDeletedFalse("Group A")).thenReturn(true);
//        assertThrows(DuplicateNameException.class, () -> service.createRatePackageGroup(dto));
//    }
//
//    @Test
//    void getAllRatePackageGroups_returnsList() {
//        when(ratePackageGroupRepository.findAll()).thenReturn(List.of(group));
//        List<RatePackageGroupDTO> result = service.getAllRatePackageGroups();
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    void getRatePackageGroupById_success() {
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(group));
//        RatePackageGroupDTO result = service.getRatePackageGroupById(1L);
//        assertEquals("Group A", result.getRatePackageGroupName());
//    }
//
//    @Test
//    void getRatePackageGroupById_notFound_throwsException() {
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> service.getRatePackageGroupById(1L));
//    }
//
//    @Test
//    void updateRatePackageGroup_success() {
//        // Given
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(group));
//        when(ratePackageGroupRepository.save(any(RatePackageGroup.class))).thenReturn(group);
//
//        // When
//        RatePackageGroupDTO result = service.updateRatePackageGroup(1L, dto);
//
//        // Then
//        assertNotNull(result);
//        assertEquals("Group A", result.getRatePackageGroupName());
//        verify(ratePackageGroupRepository).findById(1L);
//        verify(ratePackageGroupRepository).save(any(RatePackageGroup.class));
//    }
//
//    @Test
//    void updateRatePackageGroup_duplicateName_throwsException() {
//        RatePackageGroup anotherGroup = RatePackageGroup.builder()
//            .ratePackageGroupId(2L)
//            .name("Other Group")
//            .description("Other")
//            .packageType(RatePackageGroup.PackageType.SELLING)
//            .ratePackageAssociations(new ArrayList<>())
//            .build();
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(group));
//        dto.setRatePackageGroupName("Other Group");
//        when(ratePackageGroupRepository.existsByNameAndIsDeletedFalse("Other Group")).thenReturn(true);
//        assertThrows(DuplicateNameException.class, () -> service.updateRatePackageGroup(1L, dto));
//    }
//
//    @Test
//    void deleteRatePackageGroup_Success() {
//        when(ratePackageGroupRepository.existsById(10L)).thenReturn(true);
//        when(ratePackageAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(10L))
//                .thenReturn(false);
//        when(productPlanAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(10L))
//                .thenReturn(false);
//        doNothing().when(ratePackageGroupRepository).deleteById(10L);
//
//        service.deleteRatePackageGroup(10L);
//
//        verify(ratePackageGroupRepository).deleteById(10L);
//    }
//
//    @Test
//    void deleteRatePackageGroup_notFound_throwsException() {
//        when(ratePackageGroupRepository.existsById(1L)).thenReturn(false);
//        assertThrows(ResourceNotFoundException.class, () -> service.deleteRatePackageGroup(1L));
//    }
//
//    @Test
//    void deleteRatePackageGroup_referencedByAssociation_throwsException() {
//        when(ratePackageGroupRepository.existsById(10L)).thenReturn(true);
//        when(ratePackageAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(10L)).thenReturn(true);
//        assertThrows(com.xcess.ocs.exception.ForeignReferenceException.class, () -> service.deleteRatePackageGroup(10L));
//    }
//
//    @Test
//    void deleteRatePackageGroup_referencedByProductPlan_throwsException() {
//        when(ratePackageGroupRepository.existsById(10L)).thenReturn(true);
//        when(ratePackageAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(10L)).thenReturn(false);
//        when(productPlanAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(10L)).thenReturn(true);
//        assertThrows(com.xcess.ocs.exception.ForeignReferenceException.class, () -> service.deleteRatePackageGroup(10L));
//    }
//
//    @Test
//    void createRatePackageGroup_nullDto_throwsException() {
//        assertThrows(NullPointerException.class, () -> service.createRatePackageGroup(null));
//    }
//
//    @Test
//    void updateRatePackageGroup_notFound_throwsException() {
//        when(ratePackageGroupRepository.findById(999L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> service.updateRatePackageGroup(999L, dto));
//    }
//
//
//    @Test
//    void createRatePackageGroup_withInvalidAssociation_throwsException() {
//        RatePackageAssociationDTO assocDto = new RatePackageAssociationDTO();
//        assocDto.setRatePackage(999L);
//        dto.setRatePackages(List.of(assocDto));
//        when(ratePackageGroupRepository.existsByNameAndIsDeletedFalse(anyString())).thenReturn(false);
//        when(ratePackageRepository.findById(999L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> service.createRatePackageGroup(dto));
//    }
//
//    @Test
//    void updateRatePackageGroup_withInvalidAssociation_throwsException() {
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(group));
//        RatePackageAssociationDTO assocDto = new RatePackageAssociationDTO();
//        assocDto.setRatePackage(999L);
//        dto.setRatePackages(List.of(assocDto));
//        when(ratePackageRepository.findById(999L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> service.updateRatePackageGroup(1L, dto));
//    }
//
//    @Test
//    void getAllRatePackageGroups_emptyList() {
//        when(ratePackageGroupRepository.findAll()).thenReturn(Collections.emptyList());
//        List<RatePackageGroupDTO> result = service.getAllRatePackageGroups();
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void createRatePackageGroup_nullName_throwsException() {
//        dto.setRatePackageGroupName(null);
//        when(ratePackageGroupRepository.existsByNameAndIsDeletedFalse(null)).thenReturn(false);
//        assertThrows(NullPointerException.class, () -> service.createRatePackageGroup(dto));
//    }
//
//    @Test
//    void createRatePackageGroup_invalidType_throwsException() {
//        dto.setPackageType("INVALID");
//        when(ratePackageGroupRepository.existsByNameAndIsDeletedFalse(anyString())).thenReturn(false);
//        assertThrows(IllegalArgumentException.class, () -> service.createRatePackageGroup(dto));
//    }
//
//
//    @Test
//    void updateRatePackageGroup_invalidType_throwsException() {
//        when(ratePackageGroupRepository.findById(1L)).thenReturn(Optional.of(group));
//        dto.setPackageType("INVALID");
//        assertThrows(IllegalArgumentException.class, () -> service.updateRatePackageGroup(1L, dto));
//    }
//
//
//    @Test
//    void deleteRatePackageGroup_referencedByAssociation_doesNotDelete() {
//        when(ratePackageGroupRepository.existsById(10L)).thenReturn(true);
//        when(ratePackageAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(10L)).thenReturn(true);
//        assertThrows(com.xcess.ocs.exception.ForeignReferenceException.class, () -> service.deleteRatePackageGroup(10L));
//        verify(ratePackageGroupRepository, never()).deleteById(anyLong());
//    }
//
//    @Test
//    void deleteRatePackageGroup_referencedByProductPlan_doesNotDelete() {
//        when(ratePackageGroupRepository.existsById(10L)).thenReturn(true);
//        when(ratePackageAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(10L)).thenReturn(false);
//        when(productPlanAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(10L)).thenReturn(true);
//        assertThrows(com.xcess.ocs.exception.ForeignReferenceException.class, () -> service.deleteRatePackageGroup(10L));
//        verify(ratePackageGroupRepository, never()).deleteById(anyLong());
//    }
//
//    @Test
//    void getRatePackageGroupsInPages_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Create second group using builder pattern like in setUp
//        RatePackageGroup group2 = RatePackageGroup.builder()
//                .ratePackageGroupId(2L)
//                .name("Group B")
//                .description("Another test group")
//                .packageType(RatePackageGroup.PackageType.BUYING)
//                .ratePackageAssociations(new ArrayList<>())
//                .build();
//
//        Page<RatePackageGroup> groupPage = new PageImpl<>(
//                List.of(group, group2),
//                pageable,
//                2
//        );
//
//        when(ratePackageGroupRepository.findAll(pageable)).thenReturn(groupPage);
//
//        // Act
//        PageResponseDTO<RatePackageGroupDTO> result = service.getRatePackageGroupsInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(2, result.getContent().size());
//
//        // Verify first group (from setUp)
//        assertEquals("Group A", result.getContent().get(0).getRatePackageGroupName());
//        assertEquals("Test group", result.getContent().get(0).getDescription());
//        assertEquals("SELLING", result.getContent().get(0).getPackageType());
//
//        // Verify second group
//        assertEquals("Group B", result.getContent().get(1).getRatePackageGroupName());
//        assertEquals("Another test group", result.getContent().get(1).getDescription());
//        assertEquals("BUYING", result.getContent().get(1).getPackageType());
//
//        verify(ratePackageGroupRepository).findAll(pageable);
//    }
//
//    @Test
//    void getRatePackageGroupsInPages_EmptyPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<RatePackageGroup> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//
//        when(ratePackageGroupRepository.findAll(pageable)).thenReturn(emptyPage);
//
//        // Act
//        PageResponseDTO<RatePackageGroupDTO> result = service.getRatePackageGroupsInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(ratePackageGroupRepository).findAll(pageable);
//    }
//
//    @Test
//    void searchRatePackageGroups_WithAllCriteria() {
//        // Arrange
//        String searchTerm = "Group";
//        String packageType = "SELLING";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use group from setUp
//        Page<RatePackageGroup> searchResults = new PageImpl<>(
//                List.of(group),
//                pageable,
//                1
//        );
//
//        when(ratePackageGroupRepository.searchRatePackageGroups(
//                searchTerm,
//                RatePackageGroup.PackageType.SELLING,
//                pageable
//        )).thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<RatePackageGroupDTO> result = service.searchRatePackageGroups(
//                searchTerm,
//                packageType,
//                pageable
//        );
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals("Group A", result.getContent().get(0).getRatePackageGroupName());
//        assertEquals("SELLING", result.getContent().get(0).getPackageType());
//
//        verify(ratePackageGroupRepository).searchRatePackageGroups(
//                searchTerm,
//                RatePackageGroup.PackageType.SELLING,
//                pageable
//        );
//    }
//
//    @Test
//    void searchRatePackageGroups_OnlySearchTerm() {
//        // Arrange
//        String searchTerm = "Group";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use group from setUp
//        Page<RatePackageGroup> searchResults = new PageImpl<>(
//                List.of(group),
//                pageable,
//                1
//        );
//
//        when(ratePackageGroupRepository.searchRatePackageGroups(
//                searchTerm,
//                null,
//                pageable
//        )).thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<RatePackageGroupDTO> result = service.searchRatePackageGroups(
//                searchTerm,
//                null,
//                pageable
//        );
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals("Group A", result.getContent().get(0).getRatePackageGroupName());
//
//        verify(ratePackageGroupRepository).searchRatePackageGroups(searchTerm, null, pageable);
//    }
//
//    @Test
//    void searchRatePackageGroups_OnlyPackageType() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        String packageType = "BUYING";
//
//        RatePackageGroup buyingGroup = RatePackageGroup.builder()
//                .ratePackageGroupId(2L)
//                .name("Buying Group")
//                .description("Test buying group")
//                .packageType(RatePackageGroup.PackageType.BUYING)
//                .ratePackageAssociations(new ArrayList<>())
//                .build();
//
//        Page<RatePackageGroup> searchResults = new PageImpl<>(
//                List.of(buyingGroup),
//                pageable,
//                1
//        );
//
//        when(ratePackageGroupRepository.searchRatePackageGroups(
//                null,
//                RatePackageGroup.PackageType.BUYING,
//                pageable
//        )).thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<RatePackageGroupDTO> result = service.searchRatePackageGroups(
//                null,
//                packageType,
//                pageable
//        );
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals("Buying Group", result.getContent().get(0).getRatePackageGroupName());
//        assertEquals("BUYING", result.getContent().get(0).getPackageType());
//
//        verify(ratePackageGroupRepository).searchRatePackageGroups(
//                null,
//                RatePackageGroup.PackageType.BUYING,
//                pageable
//        );
//    }
//
//    @Test
//    void searchRatePackageGroups_InvalidPackageType() {
//        // Arrange
//        String searchTerm = "Group";
//        String invalidPackageType = "INVALID";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Act & Assert
//        assertThrows(IllegalArgumentException.class, () ->
//                service.searchRatePackageGroups(searchTerm, invalidPackageType, pageable)
//        );
//
//        verify(ratePackageGroupRepository, never())
//                .searchRatePackageGroups(any(), any(), any());
//    }
//
//    @Test
//    void searchRatePackageGroups_NoResults() {
//        // Arrange
//        String searchTerm = "NonExistent";
//        String packageType = "SELLING";
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Page<RatePackageGroup> emptyResults = new PageImpl<>(List.of(), pageable, 0);
//
//        when(ratePackageGroupRepository.searchRatePackageGroups(
//                searchTerm,
//                RatePackageGroup.PackageType.SELLING,
//                pageable
//        )).thenReturn(emptyResults);
//
//        // Act
//        PageResponseDTO<RatePackageGroupDTO> result = service.searchRatePackageGroups(
//                searchTerm,
//                packageType,
//                pageable
//        );
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(ratePackageGroupRepository).searchRatePackageGroups(
//                searchTerm,
//                RatePackageGroup.PackageType.SELLING,
//                pageable
//        );
//    }
//}
