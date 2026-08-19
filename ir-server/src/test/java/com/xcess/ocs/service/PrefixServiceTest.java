//package com.xcess.ocs.service;
//
//import com.xcess.ocs.dto.PrefixDTO;
//import com.xcess.ocs.dto.PageResponseDTO;
//import com.xcess.ocs.dto.search.PrefixSearchDTO;
//import com.xcess.ocs.entity.Country;
//import com.xcess.ocs.entity.Prefix;
//import com.xcess.ocs.exception.ResourceNotFoundException;
//import com.xcess.ocs.mapper.PrefixMapper;
//import com.xcess.ocs.repository.CountryRepository;
//import com.xcess.ocs.repository.PrefixRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.*;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.*;
//
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PrefixServiceTest {
//
//    @Mock
//    private PrefixRepository prefixRepository;
//
//    @Mock
//    private CountryRepository countryRepository;
//
//    @Mock
//    private PrefixMapper prefixMapper;
//
//    @InjectMocks
//    private PrefixService prefixService;
//
//    private Prefix prefix;
//    private PrefixDTO prefixDTO;
//    private Country country;
//
//    @BeforeEach
//    void setUp() {
//        // Set up a sample country
//        country = new Country();
//        country.setCountryId(10L);
//        country.setName("TestCountry");
//        country.setCountryCode("+123");
//
//        // Set up a sample prefix entity and DTO
//        prefix = new Prefix();
//        prefix.setPrefixId(1L);
//        prefix.setPrefix("12345");
//        prefix.setPrefixName("TestPrefix");
//        prefix.setCountry(country);
//
//        prefixDTO = new PrefixDTO();
//        prefixDTO.setPrefix("12345");
//        prefixDTO.setPrefixName("TestPrefix");
//        prefixDTO.setCountryName("TestCountry");
//    }
//
//    // ---------- getAllPrefixes() ----------------
//
//    @Test
//    void getAllPrefixes_Success() {
//        // Arrange
//        List<Prefix> prefixes = Arrays.asList(prefix, new Prefix());
//        when(prefixRepository.findAll()).thenReturn(prefixes);
//        when(prefixMapper.toDto(any(Prefix.class))).thenReturn(prefixDTO);
//
//        // Act
//        List<PrefixDTO> result = prefixService.getAllPrefixes();
//
//        // Assert
//        assertEquals(prefixes.size(), result.size());
//        verify(prefixRepository).findAll();
//        verify(prefixMapper, atLeast(prefixes.size())).toDto(any(Prefix.class));
//    }
//
//    // ---------- getPrefixById() ----------------
//
//    @Test
//    void getPrefixById_Success() {
//        // Arrange
//        when(prefixRepository.findById(1L)).thenReturn(Optional.of(prefix));
//        when(prefixMapper.toDto(prefix)).thenReturn(prefixDTO);
//
//        // Act
//        PrefixDTO result = prefixService.getPrefixById(1L);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(prefixDTO.getPrefix(), result.getPrefix());
//        verify(prefixRepository).findById(1L);
//    }
//
//    @Test
//    void getPrefixById_NotFound_ThrowsException() {
//        // Arrange
//        when(prefixRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        ResourceNotFoundException ex =
//                assertThrows(ResourceNotFoundException.class, () -> prefixService.getPrefixById(1L));
//        assertTrue(ex.getMessage().contains("Prefix not found with ID"));
//    }
//
//    // ---------- createPrefix() ----------------
//
//    @Test
//    void createPrefix_Success() {
//        // Arrange
//        when(prefixRepository.existsByPrefixAndIsDeletedFalse("12345")).thenReturn(false);
//        when(countryRepository.findByName("TestCountry")).thenReturn(Optional.of(country));
//        // Simulate validation: the prefix must start with the country code without '+'. For country "+123", clean code is "123"
//        // Here "12345" starts with "123", so validation passes.
//        when(prefixMapper.toEntity(prefixDTO, country)).thenReturn(prefix);
//        when(prefixRepository.save(prefix)).thenReturn(prefix);
//        when(prefixMapper.toDto(prefix)).thenReturn(prefixDTO);
//
//        // Act
//        PrefixDTO result = prefixService.createPrefix(prefixDTO);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(prefixDTO.getPrefix(), result.getPrefix());
//        verify(prefixRepository).existsByPrefixAndIsDeletedFalse("12345");
//        verify(countryRepository).findByName("TestCountry");
//        verify(prefixRepository).save(prefix);
//    }
//
//    @Test
//    void createPrefix_DuplicatePrefix_ThrowsException() {
//        // Arrange
//        when(prefixRepository.existsByPrefixAndIsDeletedFalse("12345")).thenReturn(true);
//
//        // Act & Assert
//        IllegalArgumentException ex =
//                assertThrows(IllegalArgumentException.class, () -> prefixService.createPrefix(prefixDTO));
//        assertTrue(ex.getMessage().contains("already exists"));
//        verify(prefixRepository).existsByPrefixAndIsDeletedFalse("12345");
//        verify(countryRepository, never()).findByName(any());
//    }
//
//    @Test
//    void createPrefix_CountryNotFound_ThrowsException() {
//        // Arrange
//        when(prefixRepository.existsByPrefixAndIsDeletedFalse("12345")).thenReturn(false);
//        when(countryRepository.findByName("TestCountry")).thenReturn(Optional.empty());
//
//        // Act & Assert
//        ResourceNotFoundException ex =
//                assertThrows(ResourceNotFoundException.class, () -> prefixService.createPrefix(prefixDTO));
//        assertTrue(ex.getMessage().contains("Country not found with name: TestCountry"));
//    }
//
//    @Test
//    void createPrefix_InvalidPrefixFormat_ThrowsException() {
//        // Arrange: Change prefix so that it does not start with the cleaned country code ("123")
//        prefixDTO.setPrefix("99999");
//        when(prefixRepository.existsByPrefixAndIsDeletedFalse("99999")).thenReturn(false);
//        when(countryRepository.findByName("TestCountry")).thenReturn(Optional.of(country));
//
//        // Act & Assert
//        IllegalArgumentException ex =
//                assertThrows(IllegalArgumentException.class, () -> prefixService.createPrefix(prefixDTO));
//        assertTrue(ex.getMessage().contains("must start with"));
//    }
//
//    // ---------- updatePrefix() ----------------
//
//    @Test
//    void updatePrefix_Success() {
//        // Arrange
//        PrefixDTO updateDTO = new PrefixDTO();
//        updateDTO.setPrefix("12399");
//        updateDTO.setPrefixName("UpdatedPrefix");
//        updateDTO.setCountryName("TestCountry");
//
//        // Set up the existing prefix with a different prefix to trigger the exists check
//        prefix.setPrefix("12345"); // Different from updateDTO's prefix
//        when(prefixRepository.findById(1L)).thenReturn(Optional.of(prefix));
//        when(prefixRepository.existsByPrefixAndIsDeletedFalse("12399")).thenReturn(false);
//        when(countryRepository.findByName("TestCountry")).thenReturn(Optional.of(country));
//
//        // Create a new Prefix object for the save operation to reflect updated values
//        Prefix updatedPrefix = new Prefix();
//        updatedPrefix.setPrefixId(1L);
//        updatedPrefix.setPrefix("12399");
//        updatedPrefix.setPrefixName("UpdatedPrefix");
//        updatedPrefix.setCountry(country);
//        when(prefixRepository.save(any(Prefix.class))).thenReturn(updatedPrefix);
//        when(prefixMapper.toDto(updatedPrefix)).thenReturn(updateDTO);
//
//        // Act
//        PrefixDTO result = prefixService.updatePrefix(1L, updateDTO);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals("12399", result.getPrefix());
//        assertEquals("UpdatedPrefix", result.getPrefixName());
//        verify(prefixRepository).findById(1L);
//        verify(prefixRepository).existsByPrefixAndIsDeletedFalse("12399");
//        verify(countryRepository).findByName("TestCountry");
//        verify(prefixRepository).save(any(Prefix.class));
//    }
//
//    @Test
//    void updatePrefix_DuplicatePrefix_ThrowsException() {
//        // Arrange
//        PrefixDTO updateDTO = new PrefixDTO();
//        updateDTO.setPrefix("12399");
//        updateDTO.setPrefixName("UpdatedPrefix");
//        updateDTO.setCountryName("TestCountry");
//
//        when(prefixRepository.findById(1L)).thenReturn(Optional.of(prefix));
//        // Simulate that the new prefix already exists in another record
//        when(prefixRepository.existsByPrefixAndIsDeletedFalse("12399")).thenReturn(true);
//
//        // Act & Assert
//        IllegalArgumentException ex =
//                assertThrows(IllegalArgumentException.class, () -> prefixService.updatePrefix(1L, updateDTO));
//        assertTrue(ex.getMessage().contains("already exists"));
//        verify(prefixRepository).findById(1L);
//        verify(prefixRepository).existsByPrefixAndIsDeletedFalse("12399");
//        verify(prefixRepository, never()).save(any());
//    }
//
//    @Test
//    void updatePrefix_CountryNotFound_ThrowsException() {
//        // Arrange
//        PrefixDTO updateDTO = new PrefixDTO();
//        updateDTO.setPrefix("12399");
//        updateDTO.setPrefixName("UpdatedPrefix");
//        updateDTO.setCountryName("NonExistentCountry");
//
//        when(prefixRepository.findById(1L)).thenReturn(Optional.of(prefix));
//        when(prefixRepository.existsByPrefixAndIsDeletedFalse("12399")).thenReturn(false);
//        when(countryRepository.findByName("NonExistentCountry")).thenReturn(Optional.empty());
//
//        // Act & Assert
//        ResourceNotFoundException ex =
//                assertThrows(ResourceNotFoundException.class, () -> prefixService.updatePrefix(1L, updateDTO));
//        assertTrue(ex.getMessage().contains("Country not found with name"));
//    }
//
//    @Test
//    void updatePrefix_InvalidPrefixFormat_ThrowsException() {
//        // Arrange
//        PrefixDTO updateDTO = new PrefixDTO();
//        updateDTO.setPrefix("99999");  // Does not start with country code "123"
//        updateDTO.setPrefixName("UpdatedPrefix");
//        updateDTO.setCountryName("TestCountry");
//
//        when(prefixRepository.findById(1L)).thenReturn(Optional.of(prefix));
//        when(prefixRepository.existsByPrefixAndIsDeletedFalse("99999")).thenReturn(false);
//        when(countryRepository.findByName("TestCountry")).thenReturn(Optional.of(country));
//
//        // Act & Assert
//        IllegalArgumentException ex =
//                assertThrows(IllegalArgumentException.class, () -> prefixService.updatePrefix(1L, updateDTO));
//        assertTrue(ex.getMessage().contains("must start with"));
//    }
//
//    // ---------- deletePrefix() ----------------
//
//    @Test
//    void deletePrefix_Success() {
//        // Arrange
//        when(prefixRepository.existsById(1L)).thenReturn(true);
//        doNothing().when(prefixRepository).deleteById(1L);
//
//        // Act
//        prefixService.deletePrefix(1L);
//
//        // Assert
//        verify(prefixRepository).existsById(1L);
//        verify(prefixRepository).deleteById(1L);
//    }
//
//    @Test
//    void deletePrefix_NotFound_ThrowsException() {
//        // Arrange
//        when(prefixRepository.existsById(1L)).thenReturn(false);
//
//        // Act & Assert
//        ResourceNotFoundException ex =
//                assertThrows(ResourceNotFoundException.class, () -> prefixService.deletePrefix(1L));
//        assertTrue(ex.getMessage().contains("Prefix not found with ID"));
//        verify(prefixRepository).existsById(1L);
//        verify(prefixRepository, never()).deleteById(anyLong());
//    }
//
//    // ---------- getPrefixesByCountryName() ----------------
//
//    @Test
//    void getPrefixesByCountryName_Success() {
//        // Arrange
//        when(countryRepository.findByName("TestCountry")).thenReturn(Optional.of(country));
//        List<Prefix> prefixList = Arrays.asList(prefix);
//        when(prefixRepository.findByCountryCountryId(country.getCountryId())).thenReturn(prefixList);
//        when(prefixMapper.toDto(prefix)).thenReturn(prefixDTO);
//
//        // Act
//        List<PrefixDTO> result = prefixService.getPrefixesByCountryName("TestCountry");
//
//        // Assert
//        assertEquals(1, result.size());
//        verify(countryRepository).findByName("TestCountry");
//        verify(prefixRepository).findByCountryCountryId(country.getCountryId());
//    }
//
//    @Test
//    void getPrefixesByCountryName_CountryNotFound_ThrowsException() {
//        // Arrange
//        when(countryRepository.findByName("UnknownCountry")).thenReturn(Optional.empty());
//
//        // Act & Assert
//        ResourceNotFoundException ex =
//                assertThrows(ResourceNotFoundException.class, () -> prefixService.getPrefixesByCountryName("UnknownCountry"));
//        assertTrue(ex.getMessage().contains("Country not found with name"));
//    }
//
//    @Test
//    void getAllPrefixes_EmptyList() {
//        // Arrange
//        when(prefixRepository.findAll()).thenReturn(Collections.emptyList());
//
//        // Act
//        List<PrefixDTO> result = prefixService.getAllPrefixes();
//
//        // Assert
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//        verify(prefixRepository).findAll();
//    }
//
//    @Test
//    void getPrefixesByCountryId_Success() {
//        // Arrange
//        when(countryRepository.findById(10L)).thenReturn(Optional.of(country));
//        List<Prefix> prefixList = Arrays.asList(prefix);
//        when(prefixRepository.findByCountryCountryId(10L)).thenReturn(prefixList);
//        when(prefixMapper.toDto(prefix)).thenReturn(prefixDTO);
//
//        // Act
//        List<PrefixDTO> result = prefixService.getPrefixesByCountryId(10L);
//
//        // Assert
//        assertEquals(1, result.size());
//        verify(countryRepository).findById(10L);
//        verify(prefixRepository).findByCountryCountryId(10L);
//    }
//
//    @Test
//    void getPrefixesByCountryId_CountryNotFound_ThrowsException() {
//        // Arrange
//        when(countryRepository.findById(10L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        ResourceNotFoundException ex =
//                assertThrows(ResourceNotFoundException.class, () -> prefixService.getPrefixesByCountryId(10L));
//        assertTrue(ex.getMessage().contains("Country not found with ID"));
//    }
//
//    // ---------- updatePrefixFieldsOnly() ----------------
//
//    @Test
//    void updatePrefixFieldsOnly_Success() {
//        // Arrange
//        Map<String, Object> fieldsToUpdate = new HashMap<>();
//        fieldsToUpdate.put("prefix", "12367");  // Must start with "123" for country code "+123"
//        fieldsToUpdate.put("prefixName", "RenamedPrefix");
//
//        // Assume that the existing prefix's country remains the same.
//        when(prefixRepository.findById(1L)).thenReturn(Optional.of(prefix));
//        // For validation, the prefix "12367" starts correctly with "123"
//        Prefix updatedPrefix = new Prefix();
//        updatedPrefix.setPrefixId(1L);
//        updatedPrefix.setPrefix("12367");
//        updatedPrefix.setPrefixName("RenamedPrefix");
//        updatedPrefix.setCountry(country);
//        when(prefixRepository.save(any(Prefix.class))).thenReturn(updatedPrefix);
//        when(prefixMapper.toDto(updatedPrefix)).thenReturn(new PrefixDTO() {{
//            setPrefix("12367");
//            setPrefixName("RenamedPrefix");
//            setCountryName("TestCountry");
//        }});
//
//        // Act
//        PrefixDTO result = prefixService.updatePrefixFieldsOnly(1L, fieldsToUpdate);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals("12367", result.getPrefix());
//        assertEquals("RenamedPrefix", result.getPrefixName());
//        verify(prefixRepository).findById(1L);
//        verify(prefixRepository).save(any(Prefix.class));
//    }
//
//    @Test
//    void updatePrefixFieldsOnly_InvalidPrefixFormat_ThrowsException() {
//        // Arrange
//        Map<String, Object> fieldsToUpdate = new HashMap<>();
//        fieldsToUpdate.put("prefix", "99999");  // Does not start with "123"
//        fieldsToUpdate.put("prefixName", "NewName");
//
//        when(prefixRepository.findById(1L)).thenReturn(Optional.of(prefix));
//
//        // Act & Assert
//        IllegalArgumentException ex =
//                assertThrows(IllegalArgumentException.class, () -> prefixService.updatePrefixFieldsOnly(1L, fieldsToUpdate));
//        assertTrue(ex.getMessage().contains("must start with"));
//    }
//
//    // ---------- getPrefixesInPage() ----------------
//
//    @Test
//    void getPrefixesInPage_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Prefix prefix2 = new Prefix();
//        prefix2.setPrefixId(2L);
//        prefix2.setPrefix("12399");
//        prefix2.setPrefixName("Second Prefix");
//        prefix2.setCountry(country);
//
//        PrefixDTO prefix2DTO = new PrefixDTO();
//        prefix2DTO.setPrefixId(2L);
//        prefix2DTO.setPrefix("12399");
//        prefix2DTO.setPrefixName("Second Prefix");
//        prefix2DTO.setCountryName("TestCountry");
//
//        Page<Prefix> prefixPage = new PageImpl<>(
//            List.of(prefix, prefix2),
//            pageable,
//            2
//        );
//
//        when(prefixRepository.findAll(pageable)).thenReturn(prefixPage);
//        when(prefixMapper.toDto(prefix)).thenReturn(prefixDTO);
//        when(prefixMapper.toDto(prefix2)).thenReturn(prefix2DTO);
//
//        // Act
//        PageResponseDTO<PrefixDTO> result = prefixService.getPrefixesInPage(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(2, result.getContent().size());
//        assertEquals("12345", result.getContent().get(0).getPrefix());
//        assertEquals("12399", result.getContent().get(1).getPrefix());
//
//        verify(prefixRepository).findAll(pageable);
//        verify(prefixMapper, times(2)).toDto(any(Prefix.class));
//    }
//
//    @Test
//    void getPrefixesInPage_EmptyPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Prefix> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//
//        when(prefixRepository.findAll(pageable)).thenReturn(emptyPage);
//
//        // Act
//        PageResponseDTO<PrefixDTO> result = prefixService.getPrefixesInPage(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(prefixRepository).findAll(pageable);
//        verifyNoInteractions(prefixMapper);
//    }
//
//    // ---------- searchPrefixes() ----------------
//
//    @Test
//    void searchPrefixes_Success() {
//        // Arrange
//        PrefixSearchDTO searchDTO = new PrefixSearchDTO();
//        searchDTO.setSearchTerm("Test");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use prefix from setUp() and create a second one
//        Prefix prefix2 = new Prefix();
//        prefix2.setPrefixId(2L);
//        prefix2.setPrefix("12399");
//        prefix2.setPrefixName("Test Search");
//        prefix2.setCountry(country);
//
//        PrefixDTO prefix2DTO = new PrefixDTO();
//        prefix2DTO.setPrefixId(2L);
//        prefix2DTO.setPrefix("12399");
//        prefix2DTO.setPrefixName("Test Search");
//        prefix2DTO.setCountryName("TestCountry");
//
//        Page<Prefix> searchResults = new PageImpl<>(
//            List.of(prefix, prefix2),
//            pageable,
//            2
//        );
//
//        when(prefixRepository.searchPrefixes("Test", pageable)).thenReturn(searchResults);
//        when(prefixMapper.toDto(prefix)).thenReturn(prefixDTO);
//        when(prefixMapper.toDto(prefix2)).thenReturn(prefix2DTO);
//
//        // Act
//        PageResponseDTO<PrefixDTO> result = prefixService.searchPrefixes(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(2, result.getContent().size());
//
//        // Verify first prefix (from setUp)
//        assertEquals("TestPrefix", result.getContent().get(0).getPrefixName());
//        assertEquals("12345", result.getContent().get(0).getPrefix());
//
//        // Verify second prefix
//        assertEquals("Test Search", result.getContent().get(1).getPrefixName());
//        assertEquals("12399", result.getContent().get(1).getPrefix());
//
//        verify(prefixRepository).searchPrefixes("Test", pageable);
//        verify(prefixMapper, times(2)).toDto(any(Prefix.class));
//    }
//
//    @Test
//    void searchPrefixes_NoResults() {
//        // Arrange
//        PrefixSearchDTO searchDTO = new PrefixSearchDTO();
//        searchDTO.setSearchTerm("NonExistent");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Page<Prefix> emptyResults = new PageImpl<>(List.of(), pageable, 0);
//        when(prefixRepository.searchPrefixes("NonExistent", pageable))
//            .thenReturn(emptyResults);
//
//        // Act
//        PageResponseDTO<PrefixDTO> result = prefixService.searchPrefixes(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(prefixRepository).searchPrefixes("NonExistent", pageable);
//        verifyNoInteractions(prefixMapper);
//    }
//
//    @Test
//    void searchPrefixes_NullSearchTerm() {
//        // Arrange
//        PrefixSearchDTO searchDTO = new PrefixSearchDTO();
//        searchDTO.setSearchTerm(null);
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use prefix from setUp()
//        Page<Prefix> allPrefixes = new PageImpl<>(List.of(prefix), pageable, 1);
//        when(prefixRepository.searchPrefixes(null, pageable)).thenReturn(allPrefixes);
//        when(prefixMapper.toDto(prefix)).thenReturn(prefixDTO);
//
//        // Act
//        PageResponseDTO<PrefixDTO> result = prefixService.searchPrefixes(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(1, result.getContent().size());
//        assertEquals("TestPrefix", result.getContent().get(0).getPrefixName());
//
//        verify(prefixRepository).searchPrefixes(null, pageable);
//        verify(prefixMapper).toDto(prefix);
//    }
//}
