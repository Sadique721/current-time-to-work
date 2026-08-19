//package com.xcess.ocs.service;
//
//import com.xcess.ocs.dto.PulseDTO;
//import com.xcess.ocs.dto.search.PulseSearchDTO;
//import com.xcess.ocs.dto.PageResponseDTO;
//import com.xcess.ocs.entity.Pulse;
//import com.xcess.ocs.entity.UnitType;
//import com.xcess.ocs.exception.DuplicateNameException;
//import com.xcess.ocs.exception.ForeignReferenceException;
//import com.xcess.ocs.mapper.PulseMapper;
//import com.xcess.ocs.repository.PulseRepository;
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
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static com.xcess.ocs.entity.ServiceType.*;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PulseServiceTest {
//
//    @Mock
//    private PulseRepository pulseRepository;
//
//    @Mock
//    private RatePackageRepository ratePackageRepository;
//
//    @Mock
//    private PulseMapper pulseMapper;
//
//    @InjectMocks
//    private PulseService pulseService;
//
//    private PulseDTO pulseDTO;
//    private Pulse pulse;
//
//    @BeforeEach
//    void setUp() {
//        pulseDTO = new PulseDTO();
//        pulseDTO.setPulseName("Test Pulse");
//        pulseDTO.setServiceType(VOICE);
//        pulseDTO.setUnit(UnitType.MB);
//        pulseDTO.setNoOfUnits(10);
//
//        pulse = new Pulse();
//        pulse.setPulseId(1L);
//        pulse.setPulseName("Test Pulse");
//        pulse.setServiceType(SMS);
//        pulse.setUnit(UnitType.MB);
//        pulse.setNoOfUnits(10);
//    }
//
//    @Test
//    void createPulse_Success() {
//        when(pulseRepository.existsByPulseNameAndIsDeletedFalse("Test Pulse")).thenReturn(false);
//        when(pulseRepository.save(any(Pulse.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return the saved Pulse as-is
//
//        PulseDTO result = pulseService.createPulse(pulseDTO);
//
//        assertNotNull(result);
//        assertEquals("Test Pulse", result.getPulseName());
//        assertEquals(UnitType.MB, result.getUnit());
//        assertEquals(10, result.getNoOfUnits()); // This should now pass
//
//        verify(pulseRepository).existsByPulseNameAndIsDeletedFalse("Test Pulse");
//        verify(pulseRepository).save(any(Pulse.class));
//    }
//
//
//    @Test
//    void createPulse_DuplicateName_ThrowsException() {
//        when(pulseRepository.existsByPulseNameAndIsDeletedFalse("Test Pulse")).thenReturn(true);
//
//        assertThrows(DuplicateNameException.class, () -> pulseService.createPulse(pulseDTO));
//        verify(pulseRepository).existsByPulseNameAndIsDeletedFalse("Test Pulse");
//        verify(pulseRepository, never()).save(any(Pulse.class));
//    }
//
//    @Test
//    void getAllPulses_Success() {
//        when(pulseRepository.findAll()).thenReturn(Collections.singletonList(pulse));
//
//        List<PulseDTO> result = pulseService.getAllPulses();
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals("Test Pulse", result.get(0).getPulseName());
//        verify(pulseRepository).findAll();
//    }
//
//    @Test
//    void getPulseById_Success() {
//        when(pulseRepository.findById(1L)).thenReturn(Optional.of(pulse));
//
//        PulseDTO result = pulseService.getPulseById(1L);
//
//        assertNotNull(result);
//        assertEquals("Test Pulse", result.getPulseName());
//        assertEquals(UnitType.MB, result.getUnit());
//        verify(pulseRepository).findById(1L);
//    }
//
//    @Test
//    void getPulseById_NotFound_ThrowsException() {
//        when(pulseRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(RuntimeException.class, () -> pulseService.getPulseById(1L));
//        verify(pulseRepository).findById(1L);
//    }
//
//    @Test
//    void updatePulse_Success() {
//        when(pulseRepository.findById(1L)).thenReturn(Optional.of(pulse));
//        when(pulseRepository.save(any(Pulse.class))).thenReturn(pulse);
//
//        PulseDTO result = pulseService.updatePulse(1L, pulseDTO);
//
//        assertNotNull(result);
//        assertEquals("Test Pulse", result.getPulseName());
//        assertEquals(UnitType.MB, result.getUnit());
//        verify(pulseRepository).findById(1L);
//        verify(pulseRepository).save(any(Pulse.class));
//    }
//
//    @Test
//    void updatePulse_NotFound_ThrowsException() {
//        when(pulseRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(RuntimeException.class, () -> pulseService.updatePulse(1L, pulseDTO));
//        verify(pulseRepository).findById(1L);
//        verify(pulseRepository, never()).save(any(Pulse.class));
//    }
//
//    @Test
//    void updatePulse_DuplicateName_ThrowsException() {
//        Pulse existingPulse = new Pulse();
//        existingPulse.setPulseName("Old Name");
//
//        when(pulseRepository.findById(1L)).thenReturn(Optional.of(existingPulse));
//        when(pulseRepository.existsByPulseNameAndIsDeletedFalse("Test Pulse")).thenReturn(true);
//
//        assertThrows(DuplicateNameException.class, () -> pulseService.updatePulse(1L, pulseDTO));
//        verify(pulseRepository).findById(1L);
//        verify(pulseRepository, never()).save(any(Pulse.class));
//    }
//
//    @Test
//    void deletePulse_Success() {
//        when(pulseRepository.existsById(1L)).thenReturn(true);
//        when(ratePackageRepository.existsByPulse_PulseIdAndIsDeletedFalse(1L)).thenReturn(false);
//        doNothing().when(pulseRepository).deleteById(1L);
//
//        pulseService.deletePulse(1L);
//
//        verify(pulseRepository).existsById(1L);
//        verify(ratePackageRepository).existsByPulse_PulseIdAndIsDeletedFalse(1L);
//        verify(pulseRepository).deleteById(1L);
//    }
//
//    @Test
//    void deletePulse_NotFound_ThrowsException() {
//        when(pulseRepository.existsById(1L)).thenReturn(false);
//
//        assertThrows(RuntimeException.class, () -> pulseService.deletePulse(1L));
//        verify(pulseRepository).existsById(1L);
//        verify(pulseRepository, never()).deleteById(anyLong());
//    }
//
//    @Test
//    void deletePulse_ReferencedByRatePackage_ThrowsException() {
//        when(pulseRepository.existsById(1L)).thenReturn(true);
//        when(ratePackageRepository.existsByPulse_PulseIdAndIsDeletedFalse(1L)).thenReturn(true);
//
//        assertThrows(ForeignReferenceException.class, () -> pulseService.deletePulse(1L));
//        verify(pulseRepository).existsById(1L);
//        verify(ratePackageRepository).existsByPulse_PulseIdAndIsDeletedFalse(1L);
//        verify(pulseRepository, never()).deleteById(anyLong());
//    }
//
//    @Test
//    void getPulsesInPagesByPost_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use pulse from setUp() and create a second one
//        Pulse pulse2 = new Pulse();
//        pulse2.setPulseId(2L);
//        pulse2.setPulseName("Second Pulse");
//        pulse2.setServiceType(USAGE);
//        pulse2.setUnit(UnitType.GB);
//        pulse2.setNoOfUnits(5);
//
//        Page<Pulse> pulsePage = new PageImpl<>(
//            List.of(pulse, pulse2),
//            pageable,
//            2
//        );
//
//        when(pulseRepository.findAll(pageable)).thenReturn(pulsePage);
//
//        PulseDTO pulse2DTO = new PulseDTO();
//        pulse2DTO.setPulseId(2L);
//        pulse2DTO.setPulseName("Second Pulse");
//        pulse2DTO.setServiceType(USAGE);
//        pulse2DTO.setUnit(UnitType.GB);
//        pulse2DTO.setNoOfUnits(5);
//
//
//        // Act
//        PageResponseDTO<PulseDTO> result = pulseService.getPulsesInPagesByPost(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(2, result.getContent().size());
//
//        // Verify first pulse (from setUp)
//        assertEquals("Test Pulse", result.getContent().get(0).getPulseName());
//        assertEquals(SMS, result.getContent().get(0).getServiceType());
//
//        // Verify second pulse
//        assertEquals("Second Pulse", result.getContent().get(1).getPulseName());
//        assertEquals(USAGE, result.getContent().get(1).getServiceType());
//
//        verify(pulseRepository).findAll(pageable);
//    }
//
//    @Test
//    void getPulsesInPagesByPost_EmptyPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Pulse> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//
//        when(pulseRepository.findAll(pageable)).thenReturn(emptyPage);
//
//        // Act
//        PageResponseDTO<PulseDTO> result = pulseService.getPulsesInPagesByPost(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(pulseRepository).findAll(pageable);
//        verifyNoInteractions(pulseMapper);
//    }
//
//    @Test
//    void searchPulses_WithAllCriteria() {
//        // Arrange
//        PulseSearchDTO searchDTO = new PulseSearchDTO();
//        searchDTO.setSearchTerm("Test");
//        searchDTO.setServiceType(SMS);
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use pulse from setUp()
//        Page<Pulse> searchResults = new PageImpl<>(
//            List.of(pulse),
//            pageable,
//            1
//        );
//
//        when(pulseRepository.searchPulses("Test", SMS, pageable))
//            .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<PulseDTO> result = pulseService.searchPulses(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals("Test Pulse", result.getContent().get(0).getPulseName());
//        assertEquals(SMS, result.getContent().get(0).getServiceType());
//
//        verify(pulseRepository).searchPulses("Test", SMS, pageable);
//    }
//
//    @Test
//    void searchPulses_OnlySearchTerm() {
//        // Arrange
//        PulseSearchDTO searchDTO = new PulseSearchDTO();
//        searchDTO.setSearchTerm("Test");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use pulse from setUp()
//        Page<Pulse> searchResults = new PageImpl<>(
//            List.of(pulse),
//            pageable,
//            1
//        );
//
//        when(pulseRepository.searchPulses("Test", null, pageable))
//            .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<PulseDTO> result = pulseService.searchPulses(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals("Test Pulse", result.getContent().get(0).getPulseName());
//
//        verify(pulseRepository).searchPulses("Test", null, pageable);
//    }
//
//    @Test
//    void searchPulses_OnlyServiceType() {
//        // Arrange
//        PulseSearchDTO searchDTO = new PulseSearchDTO();
//        searchDTO.setServiceType(SMS);
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use pulse from setUp() but with SMS service type
//        pulse.setServiceType(SMS);
//        Page<Pulse> searchResults = new PageImpl<>(
//            List.of(pulse),
//            pageable,
//            1
//        );
//
//        when(pulseRepository.searchPulses(null, SMS, pageable))
//            .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<PulseDTO> result = pulseService.searchPulses(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals(SMS, result.getContent().get(0).getServiceType());
//
//        verify(pulseRepository).searchPulses(null, SMS, pageable);
//    }
//
//    @Test
//    void searchPulses_NoResults() {
//        // Arrange
//        PulseSearchDTO searchDTO = new PulseSearchDTO();
//        searchDTO.setSearchTerm("NonExistent");
//        searchDTO.setServiceType(VOICE);
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Page<Pulse> emptyResults = new PageImpl<>(List.of(), pageable, 0);
//
//        when(pulseRepository.searchPulses("NonExistent", VOICE, pageable))
//            .thenReturn(emptyResults);
//
//        // Act
//        PageResponseDTO<PulseDTO> result = pulseService.searchPulses(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(pulseRepository).searchPulses("NonExistent", VOICE, pageable);
//        verifyNoInteractions(pulseMapper);
//    }
//}
