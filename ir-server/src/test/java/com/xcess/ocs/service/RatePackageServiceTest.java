//package com.xcess.ocs.service;
//
//import com.xcess.ocs.cache.RatePackageCache;
//import com.xcess.ocs.dto.PageResponseDTO;
//import com.xcess.ocs.dto.RatePackageDTO;
//import com.xcess.ocs.dto.search.RatePackageSearchDTO;
//import com.xcess.ocs.entity.*;
//import com.xcess.ocs.exception.DuplicateNameException;
//import com.xcess.ocs.exception.ForeignReferenceException;
//import com.xcess.ocs.exception.ResourceNotFoundException;
//import com.xcess.ocs.repository.PulseRepository;
//import com.xcess.ocs.repository.RatePackageAssociationRepository;
//import com.xcess.ocs.repository.RatePackageRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.lang.reflect.Field;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class RatePackageServiceTest {
//
//    @Mock
//    private RatePackageRepository ratePackageRepository;
//
//    @Mock
//    private PulseRepository pulseRepository;
//
//    @Mock
//    private RatePackageAssociationRepository ratePackageAssociationRepository;
//
//    @Mock
//    private RatePackageCache ratePackageCache;
//
//    @InjectMocks
//    private RatePackageService ratePackageService;
//
//    private RatePackage ratePackage;
//    private RatePackageDTO dto;
//    private Pulse pulse;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        ratePackageService = new RatePackageService(ratePackageRepository, pulseRepository, ratePackageCache);
//        // Use reflection to inject the mock into the private field
//        Field field = RatePackageService.class.getDeclaredField("ratePackageAssociationRepository");
//        field.setAccessible(true);
//        field.set(ratePackageService, ratePackageAssociationRepository);
//
//
//        pulse = new Pulse();
//        pulse.setPulseId(1L);
//        pulse.setPulseName("Test Pulse");
//
//        ratePackage = RatePackage.builder()
//                .ratePackageId(1L)
//                .packageName("Test Package")
//                .packageDesc("Test Description")
//                .type(Type.SELLING)
//                .serviceType(ServiceType.VOICE)
//                .pulse(pulse)
//                .ratePackageType(RatePackageType.SOURCE_DESTINATION_BASED)
//                .rounding(Rounding.DEFAULT)
//                .priceRounding(Rounding.DEFAULT)
//                .build();
//
//        dto = new RatePackageDTO();
//        dto.setPackageName("Test Package");
//        dto.setPackageDesc("Test Description");
//        dto.setType(Type.SELLING);
//        dto.setServiceType(ServiceType.VOICE);
//        dto.setPulseId(1L);
//        dto.setRatePackageType(RatePackageType.SOURCE_DESTINATION_BASED);
//        dto.setRounding(Rounding.DEFAULT);
//        dto.setPriceRounding(Rounding.DEFAULT);
//    }
//
//    @Test
//    void createRatePackage_Success() {
//        when(ratePackageRepository.existsByPackageNameAndIsDeletedFalse(anyString())).thenReturn(false);
//        when(pulseRepository.findById(1L)).thenReturn(Optional.of(pulse));
//        when(ratePackageRepository.save(any(RatePackage.class))).thenReturn(ratePackage);
//
//        RatePackageDTO result = ratePackageService.createRatePackage(dto);
//
//        assertNotNull(result);
//        assertEquals("Test Package", result.getPackageName());
//        assertEquals(ServiceType.VOICE, result.getServiceType());
//        assertEquals(Type.SELLING, result.getType());
//        verify(ratePackageRepository).save(any(RatePackage.class));
//    }
//
//    @Test
//    void createRatePackage_DuplicateName() {
//        when(ratePackageRepository.existsByPackageNameAndIsDeletedFalse(anyString())).thenReturn(true);
//
//        assertThrows(DuplicateNameException.class, () -> ratePackageService.createRatePackage(dto));
//        verify(pulseRepository, never()).findById(any());
//    }
//
//    @Test
//    void createRatePackage_PulseNotFound() {
//        when(ratePackageRepository.existsByPackageNameAndIsDeletedFalse(anyString())).thenReturn(false);
//        when(pulseRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> ratePackageService.createRatePackage(dto));
//    }
//
//    @Test
//    void createRatePackage_NullName() {
//        dto.setPackageName(null);
//        when(ratePackageRepository.existsByPackageNameAndIsDeletedFalse(null)).thenReturn(false);
//        when(pulseRepository.findById(1L)).thenReturn(Optional.of(pulse));
//        assertThrows(NullPointerException.class, () -> ratePackageService.createRatePackage(dto));
//    }
//
//    @Test
//    void getAllRatePackages_Success() {
//        when(ratePackageRepository.findAll()).thenReturn(Arrays.asList(ratePackage));
//
//        List<RatePackageDTO> results = ratePackageService.getAllRatePackages();
//
//        assertNotNull(results);
//        assertEquals(1, results.size());
//        assertEquals("Test Package", results.get(0).getPackageName());
//        verify(ratePackageRepository).findAll();
//    }
//
//    @Test
//    void getAllRatePackages_EmptyList() {
//        when(ratePackageRepository.findAll()).thenReturn(Arrays.asList());
//        List<RatePackageDTO> result = ratePackageService.getAllRatePackages();
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void getPackageById_Success() {
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.of(ratePackage));
//
//        RatePackageDTO result = ratePackageService.getPackageById(1L);
//
//        assertNotNull(result);
//        assertEquals("Test Package", result.getPackageName());
//        verify(ratePackageRepository).findById(1L);
//    }
//
//    @Test
//    void getPackageById_NotFound() {
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(RuntimeException.class, () -> ratePackageService.getPackageById(1L));
//    }
//
//    @Test
//    void updateRatePackage_Success() {
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.of(ratePackage));
//        when(pulseRepository.findById(1L)).thenReturn(Optional.of(pulse));
//        when(ratePackageRepository.save(any(RatePackage.class))).thenReturn(ratePackage);
//
//        RatePackageDTO result = ratePackageService.updateRatePackage(1L, dto);
//
//        assertNotNull(result);
//        assertEquals("Test Package", result.getPackageName());
//        verify(ratePackageRepository).save(any(RatePackage.class));
//    }
//
//    @Test
//    void updateRatePackage_NotFound() {
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> ratePackageService.updateRatePackage(1L, dto));
//    }
//
//    @Test
//    void updateRatePackage_DuplicateName() {
//        RatePackage existing = RatePackage.builder()
//                .ratePackageId(1L)
//                .packageName("Old Name")
//                .pulse(pulse)
//                .build();
//        dto.setPackageName("New Name");
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.of(existing));
//        when(ratePackageRepository.existsByPackageNameAndIsDeletedFalse("New Name")).thenReturn(true);
//        assertThrows(DuplicateNameException.class, () -> ratePackageService.updateRatePackage(1L, dto));
//    }
//
//    @Test
//    void updateRatePackage_PulseNotFound() {
//        RatePackage existing = RatePackage.builder()
//                .ratePackageId(1L)
//                .packageName("Test Package")
//                .pulse(pulse)
//                .build();
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.of(existing));
//        when(pulseRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> ratePackageService.updateRatePackage(1L, dto));
//    }
//
//    @Test
//    void updateRatePackage_NullName() {
//        RatePackage existing = RatePackage.builder()
//                .ratePackageId(1L)
//                .packageName("Test Package")
//                .pulse(pulse)
//                .build();
//        dto.setPackageName(null);
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.of(existing));
//        when(ratePackageRepository.existsByPackageNameAndIsDeletedFalse(null)).thenReturn(false);
//        when(pulseRepository.findById(1L)).thenReturn(Optional.of(pulse));
//        assertThrows(NullPointerException.class, () -> ratePackageService.updateRatePackage(1L, dto));
//    }
//
//    @Test
//    void deleteRatePackage_Success() {
//        Long id = 1L;
//
//        when(ratePackageRepository.existsById(id)).thenReturn(true);
//        when(ratePackageAssociationRepository.existsByRatePackage_RatePackageIdAndIsDeletedFalse(id))
//                .thenReturn(false);
//
//        ratePackageService.deleteRatePackage(id);
//
//        verify(ratePackageRepository).deleteById(id);
//    }
//
//    @Test
//    void deleteRatePackage_NotFound() {
//        when(ratePackageRepository.existsById(99L)).thenReturn(false);
//        assertThrows(ResourceNotFoundException.class, () -> ratePackageService.deleteRatePackage(99L));
//    }
//
//    @Test
//    void deleteRatePackage_Referenced() {
//        when(ratePackageRepository.existsById(2L)).thenReturn(true);
//        when(ratePackageAssociationRepository.existsByRatePackage_RatePackageIdAndIsDeletedFalse(2L)).thenReturn(true);
//
//        assertThrows(ForeignReferenceException.class, () -> ratePackageService.deleteRatePackage(2L));
//
//        verify(ratePackageRepository, never()).deleteById(any());
//    }
//
//    @Test
//    void getRatePackagesByType_Success() {
//        when(ratePackageRepository.findByRatePackageType(RatePackageType.SOURCE_DESTINATION_BASED))
//                .thenReturn(Optional.of(Arrays.asList(ratePackage)));
//
//        List<RatePackageDTO> results = ratePackageService.getRatePackagesByType(RatePackageType.SOURCE_DESTINATION_BASED);
//
//        assertNotNull(results);
//        assertEquals(1, results.size());
//        assertEquals(RatePackageType.SOURCE_DESTINATION_BASED, results.get(0).getRatePackageType());
//        verify(ratePackageRepository).findByRatePackageType(RatePackageType.SOURCE_DESTINATION_BASED);
//    }
//
//    @Test
//    void getRatePackagesByType_NotFound() {
//        when(ratePackageRepository.findByRatePackageType(any()))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class,
//                () -> ratePackageService.getRatePackagesByType(RatePackageType.SOURCE_DESTINATION_BASED));
//    }
//
//    @Test
//    void getRatePackagesInPages_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Create second rate package using builder pattern
//        RatePackage ratePackage2 = RatePackage.builder()
//                .ratePackageId(2L)
//                .packageName("Second Package")
//                .packageDesc("Second Description")
//                .type(Type.SELLING)
//                .serviceType(ServiceType.SMS)
//                .pulse(pulse)
//                .ratePackageType(RatePackageType.SOURCE_DESTINATION_BASED)
//                .rounding(Rounding.DEFAULT)
//                .priceRounding(Rounding.DEFAULT)
//                .build();
//
//        Page<RatePackage> packagePage = new PageImpl<>(
//                List.of(ratePackage, ratePackage2),
//                pageable,
//                2
//        );
//
//        when(ratePackageRepository.findAll(pageable)).thenReturn(packagePage);
//
//        // Act
//        PageResponseDTO<RatePackageDTO> result = ratePackageService.getRatePackagesInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(2, result.getContent().size());
//
//        // Verify first package (from setUp)
//        assertEquals("Test Package", result.getContent().get(0).getPackageName());
//        assertEquals(ServiceType.VOICE, result.getContent().get(0).getServiceType());
//
//        // Verify second package
//        assertEquals("Second Package", result.getContent().get(1).getPackageName());
//        assertEquals(ServiceType.SMS, result.getContent().get(1).getServiceType());
//
//        verify(ratePackageRepository).findAll(pageable);
//    }
//
//    @Test
//    void getRatePackagesInPages_EmptyPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<RatePackage> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//
//        when(ratePackageRepository.findAll(pageable)).thenReturn(emptyPage);
//
//        // Act
//        PageResponseDTO<RatePackageDTO> result = ratePackageService.getRatePackagesInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(ratePackageRepository).findAll(pageable);
//    }
//
//    @Test
//    void searchRatePackages_WithAllCriteria() {
//        // Arrange
//        RatePackageSearchDTO searchDTO = new RatePackageSearchDTO();
//        searchDTO.setSearchTerm("Test");
//        searchDTO.setServiceType(ServiceType.VOICE);
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use ratePackage from setUp
//        Page<RatePackage> searchResults = new PageImpl<>(
//                List.of(ratePackage),
//                pageable,
//                1
//        );
//
//        when(ratePackageRepository.searchRatePackages("Test", ServiceType.VOICE, pageable))
//                .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<RatePackageDTO> result = ratePackageService.searchRatePackages(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals("Test Package", result.getContent().get(0).getPackageName());
//        assertEquals(ServiceType.VOICE, result.getContent().get(0).getServiceType());
//
//        verify(ratePackageRepository).searchRatePackages("Test", ServiceType.VOICE, pageable);
//    }
//
//    @Test
//    void searchRatePackages_OnlySearchTerm() {
//        // Arrange
//        RatePackageSearchDTO searchDTO = new RatePackageSearchDTO();
//        searchDTO.setSearchTerm("Test");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use ratePackage from setUp
//        Page<RatePackage> searchResults = new PageImpl<>(
//                List.of(ratePackage),
//                pageable,
//                1
//        );
//
//        when(ratePackageRepository.searchRatePackages("Test", null, pageable))
//                .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<RatePackageDTO> result = ratePackageService.searchRatePackages(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals("Test Package", result.getContent().get(0).getPackageName());
//
//        verify(ratePackageRepository).searchRatePackages("Test", null, pageable);
//    }
//
//    @Test
//    void searchRatePackages_OnlyServiceType() {
//        // Arrange
//        RatePackageSearchDTO searchDTO = new RatePackageSearchDTO();
//        searchDTO.setServiceType(ServiceType.SMS);
//        Pageable pageable = PageRequest.of(0, 10);
//
//        RatePackage smsPackage = RatePackage.builder()
//                .ratePackageId(2L)
//                .packageName("SMS Package")
//                .serviceType(ServiceType.SMS)
//                .pulse(pulse)
//                .build();
//
//        Page<RatePackage> searchResults = new PageImpl<>(
//                List.of(smsPackage),
//                pageable,
//                1
//        );
//
//        when(ratePackageRepository.searchRatePackages(null, ServiceType.SMS, pageable))
//                .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<RatePackageDTO> result = ratePackageService.searchRatePackages(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals("SMS Package", result.getContent().get(0).getPackageName());
//        assertEquals(ServiceType.SMS, result.getContent().get(0).getServiceType());
//
//        verify(ratePackageRepository).searchRatePackages(null, ServiceType.SMS, pageable);
//    }
//
//    @Test
//    void searchRatePackages_NoResults() {
//        // Arrange
//        RatePackageSearchDTO searchDTO = new RatePackageSearchDTO();
//        searchDTO.setSearchTerm("NonExistent");
//        searchDTO.setServiceType(ServiceType.VOICE);
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Page<RatePackage> emptyResults = new PageImpl<>(List.of(), pageable, 0);
//
//        when(ratePackageRepository.searchRatePackages("NonExistent", ServiceType.VOICE, pageable))
//                .thenReturn(emptyResults);
//
//        // Act
//        PageResponseDTO<RatePackageDTO> result = ratePackageService.searchRatePackages(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(ratePackageRepository).searchRatePackages("NonExistent", ServiceType.VOICE, pageable);
//    }
//}
