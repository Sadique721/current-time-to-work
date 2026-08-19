//package com.xcess.ocs.service;
//
//import com.xcess.ocs.dto.RateDetailDTO;
//import com.xcess.ocs.dto.PageResponseDTO;
//import com.xcess.ocs.entity.RateDetails;
//import com.xcess.ocs.entity.RateDetailsHistory;
//import com.xcess.ocs.entity.RatePackage;
//import com.xcess.ocs.entity.Country;
//import com.xcess.ocs.exception.ResourceNotFoundException;
//import com.xcess.ocs.repository.CountryRepository;
//import com.xcess.ocs.repository.RateDetailsHistoryRepository;
//import com.xcess.ocs.repository.RateDetailsRepository;
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
//import org.springframework.web.multipart.MultipartFile;
//
//import java.lang.reflect.Field;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class RateDetailsServiceTest {
//    @Mock
//    private RateDetailsRepository rateDetailsRepository;
//    @Mock
//    private RatePackageRepository ratePackageRepository;
//    @Mock
//    private RateDetailsHistoryRepository rateDetailsHistoryRepository;
//    @Mock
//    private CountryRepository countryRepository;
//
//    @InjectMocks
//    private RateDetailsService service;
//
//    private RatePackage ratePackage;
//    private RateDetailDTO validDto;
//    private RateDetails savedDetail;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        ratePackage = new RatePackage();
//        ratePackage.setRatePackageId(1L);
//
//        validDto = RateDetailDTO.builder()
//                .destinationPrefix("91824")
//                .destinationPrefixName("Test")
//                .destinationCountryCode("91")
//                .destinationCountryName("India")
//                .rate(0.05)
//                .startTime(LocalDateTime.now())
//                .endTime(LocalDateTime.now().plusDays(1))
//                .ratePackageId(1L)
//                .rateDetailsId(100L)
//                .build();
//
//        savedDetail = RateDetails.builder()
//                .rateDetailsId(100L)
//                .destinationPrefix("91824")
//                .destinationCountryCode("91")
//                .destinationCountryName("India")
//                .destinationPrefixName("Test")
//                .rate(0.05)
//                .startTime(validDto.getStartTime())
//                .endTime(validDto.getEndTime())
//                .ratePackage(ratePackage)
//                .currentVersion(1)
//                .build();
//
//        // Inject the mock for the @Autowired field
//        Field field = service.getClass().getDeclaredField("rateDetailsHistoryRepository");
//        field.setAccessible(true);
//        field.set(service, rateDetailsHistoryRepository);
//    }
//
//    @Test
//    void createRateDetail_success() {
//        // Arrange
//        Country india = new Country();
//        india.setCountryId(1L);
//        india.setName("India");
//        india.setCountryCode("91");
//
//        when(rateDetailsRepository.findMaxCurrentVersionByRatePackageId(anyLong())).thenReturn(null);
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.of(ratePackage));
//        when(rateDetailsRepository.existsByDestinationPrefixAndRatePackageRatePackageId(anyString(), anyLong()))
//                .thenReturn(false);
//        when(rateDetailsRepository.save(any(RateDetails.class))).thenReturn(savedDetail);
//        when(countryRepository.findById(1L)).thenReturn(Optional.of(india));
//        when(countryRepository.findAll()).thenReturn(List.of(india)); // Add mock country data
//
//        // Act
//        List<RateDetailDTO> result = service.createRateDetail(1L, Arrays.asList(validDto));
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        assertEquals(savedDetail.getRateDetailsId(), result.get(0).getRateDetailsId());
//        assertEquals("India", result.get(0).getDestinationCountryName());
//        assertEquals("91", result.get(0).getDestinationCountryCode());
//
//        // Verify interactions
//        verify(rateDetailsRepository).findMaxCurrentVersionByRatePackageId(anyLong());
//        verify(rateDetailsRepository).existsByDestinationPrefixAndRatePackageRatePackageId(anyString(), anyLong());
//        verify(rateDetailsRepository).save(any(RateDetails.class));
//        verify(countryRepository).findAll();
//    }
//
//    @Test
//    void createRateDetail_ratePackageNotFound_throws() {
//        when(ratePackageRepository.findById(99L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> service.createRateDetail(99L, Collections.singletonList(validDto)));
//    }
//
//    @Test
//    void createRateDetail_duplicateEntry_throws() {
//        // Arrange
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.of(ratePackage));
//        when(rateDetailsRepository.existsByDestinationPrefixAndRatePackageRatePackageId("91824", 1L))
//                .thenReturn(true);
//
//        // Act & Assert
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> service.createRateDetail(1L, Collections.singletonList(validDto)));
//        assertTrue(exception.getMessage().contains("already exists"));
//    }
//
//    @Test
//    void getAllRateDetails_emptyList() {
//        // Arrange
//        when(rateDetailsRepository.findAll()).thenReturn(Collections.emptyList());
//        when(countryRepository.findAll()).thenReturn(Collections.emptyList());
//
//        // Act
//        List<RateDetailDTO> result = service.getAllRateDetails();
//
//        // Assert
//        assertTrue(result.isEmpty());
//        verify(rateDetailsRepository).findAll();
//        verify(countryRepository).findAll();
//    }
//
//    @Test
//    void getAllRateDetails_returnsMappedDTOs() {
//        // Arrange
//        when(rateDetailsRepository.findAll()).thenReturn(Arrays.asList(savedDetail));
//        when(countryRepository.findAll()).thenReturn(Collections.emptyList());
//
//        // Act
//        List<RateDetailDTO> result = service.getAllRateDetails();
//
//        // Assert
//        assertEquals(1, result.size());
//        assertEquals(savedDetail.getRateDetailsId(), result.get(0).getRateDetailsId());
//        assertEquals(savedDetail.getDestinationPrefix(), result.get(0).getDestinationPrefix());
//        verify(rateDetailsRepository).findAll();
//        verify(countryRepository).findAll();
//    }
//
//    @Test
//    void getRateDetailById_success() {
//        RateDetails detail = new RateDetails();
//        detail.setRateDetailsId(300L);
//        when(rateDetailsRepository.findById(1L)).thenReturn(Optional.of(detail));
//
//        RateDetailDTO dto = service.getRateDetailById(1L);
//        assertEquals(300L, dto.getRateDetailsId());
//    }
//
//    @Test
//    void getRateDetailById_notFound() {
//        when(rateDetailsRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> service.getRateDetailById(1L));
//    }
//
//    @Test
//    void updateRateDetail_notFound_throws() {
//        when(rateDetailsRepository.findById(400L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> service.updateRateDetail(400L, validDto));
//    }
//
//    @Test
//    void updateRateDetail_success() {
//        // Arrange
//        Country india = new Country();
//        india.setCountryId(1L);
//        india.setName("India");
//        india.setCountryCode("91");
//
//        when(rateDetailsRepository.findById(100L)).thenReturn(Optional.of(savedDetail));
//        when(ratePackageRepository.findById(1L)).thenReturn(Optional.of(ratePackage)); // Add this mock
//        when(rateDetailsRepository.save(any(RateDetails.class))).thenReturn(savedDetail);
//        when(countryRepository.findAll()).thenReturn(List.of(india));
//
//        validDto.setRateDetailsId(100L);
//        validDto.setDestinationPrefixName("Updated Test");
//        validDto.setRatePackageId(1L); // Make sure this matches the mocked ID
//
//        // Act
//        RateDetailDTO result = service.updateRateDetail(100L, validDto);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(100L, result.getRateDetailsId());
//        assertEquals("Updated Test", result.getDestinationPrefixName());
//
//        // Verify interactions
//        verify(rateDetailsRepository).findById(100L);
//        verify(ratePackageRepository, times(2)).findById(1L);
//        verify(rateDetailsRepository).save(any(RateDetails.class));
//        verify(countryRepository).findAll();
//    }
//
//    @Test
//    void processFile_unsupportedFileType_throws() {
//        MultipartFile file = mock(MultipartFile.class);
//        when(file.isEmpty()).thenReturn(false);
//        when(file.getOriginalFilename()).thenReturn("rates.txt");
//        assertThrows(IllegalArgumentException.class, () -> service.processFile(file, 1L));
//    }
//
//    @Test
//    void getRateHistoryByRateDetailsId_returnsHistoryList() {
//        RateDetailsHistory history = new RateDetailsHistory();
//        when(rateDetailsHistoryRepository.findByRateDetailsId(10L)).thenReturn(Arrays.asList(history));
//        List<RateDetailsHistory> result = service.getRateHistoryByRateDetailsId(10L);
//        assertEquals(1, result.size());
//    }
//
//    @Test
//    void deleteRateDetail_success() {
//        when(rateDetailsRepository.existsById(5L)).thenReturn(true);
//        service.deleteRateDetail(5L);
//        verify(rateDetailsRepository).deleteById(5L);
//    }
//
//    @Test
//    void deleteRateDetail_notFound() {
//        when(rateDetailsRepository.existsById(6L)).thenReturn(false);
//        assertThrows(ResourceNotFoundException.class, () -> service.deleteRateDetail(6L));
//    }
//
//    @Test
//    void getRateHistoryByRateDetailsId_invokesRepo() {
//        when(rateDetailsHistoryRepository.findByRateDetailsId(7L)).thenReturn(Collections.emptyList());
//        service.getRateHistoryByRateDetailsId(7L);
//        verify(rateDetailsHistoryRepository).findByRateDetailsId(7L);
//    }
//
//    @Test
//    void processFile_invalidFile_throws() {
//        MultipartFile file = mock(MultipartFile.class);
//        when(file.isEmpty()).thenReturn(true);
//        assertThrows(IllegalArgumentException.class, () -> service.processFile(file, 1L));
//    }
//
//    @Test
//    void deleteRateDetail_repositoryThrowsException() {
//        when(rateDetailsRepository.existsById(5L)).thenReturn(true);
//        doThrow(new RuntimeException("DB error")).when(rateDetailsRepository).deleteById(5L);
//        assertThrows(RuntimeException.class, () -> service.deleteRateDetail(5L));
//    }
//
//    @Test
//    void getRateDetailsInPages_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//
//        List<RateDetails> rateDetails = Arrays.asList(savedDetail);
//        Page<RateDetails> rateDetailsPage = new PageImpl<>(rateDetails, pageable, 1);
//
//        when(rateDetailsRepository.findAll(pageable)).thenReturn(rateDetailsPage);
//
//        // Act
//        PageResponseDTO<RateDetailDTO> result = service.getRateDetailsInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals(savedDetail.getRateDetailsId(), result.getContent().get(0).getRateDetailsId());
//        assertEquals(savedDetail.getDestinationPrefix(), result.getContent().get(0).getDestinationPrefix());
//
//        verify(rateDetailsRepository).findAll(pageable);
//    }
//
//    @Test
//    void getRateDetailsInPages_EmptyPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<RateDetails> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
//
//        when(rateDetailsRepository.findAll(pageable)).thenReturn(emptyPage);
//
//        // Act
//        PageResponseDTO<RateDetailDTO> result = service.getRateDetailsInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.getContent().isEmpty());
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//
//        verify(rateDetailsRepository).findAll(pageable);
//    }
//
//    @Test
//    void getRateDetailsInPages_MultiplePages() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 1);
//
//        RateDetails detail2 = RateDetails.builder()
//                .rateDetailsId(101L)
//                .destinationPrefix("456")
//                .destinationPrefixName("Test 2")
//                .rate(0.10)
//                .startTime(LocalDateTime.now())
//                .endTime(LocalDateTime.now().plusDays(1))
//                .ratePackage(ratePackage)
//                .currentVersion(1)
//                .build();
//
//        List<RateDetails> rateDetails = Arrays.asList(savedDetail);
//        Page<RateDetails> rateDetailsPage = new PageImpl<>(rateDetails, pageable, 2);
//
//        when(rateDetailsRepository.findAll(pageable)).thenReturn(rateDetailsPage);
//
//        // Act
//        PageResponseDTO<RateDetailDTO> result = service.getRateDetailsInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(2, result.getPageDetails().getTotalPages());
//
//        verify(rateDetailsRepository).findAll(pageable);
//    }
//}
