//package com.xcess.ocs.service;
//
//import com.xcess.ocs.dto.CountryDTO;
//import com.xcess.ocs.dto.CountryResponseDTO;
//import com.xcess.ocs.dto.PageResponseDTO;
//import com.xcess.ocs.dto.search.CountrySearchDTO;
//import com.xcess.ocs.entity.Country;
//import com.xcess.ocs.exception.ForeignReferenceException;
//import com.xcess.ocs.exception.ResourceNotFoundException;
//import com.xcess.ocs.mapper.CountryMapper;
//import com.xcess.ocs.repository.CountryRepository;
//import com.xcess.ocs.repository.PrefixRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CountryServiceTest {
//
//    @Mock
//    private CountryRepository countryRepository;
//
//    @Mock
//    private PrefixRepository prefixRepository;
//
//    @Mock
//    private CountryMapper countryMapper;
//
//    @InjectMocks
//    private CountryService countryService;
//
//    private CountryDTO countryDTO;
//    private Country country;
//
//    @BeforeEach
//    void setUp() {
//        countryDTO = new CountryDTO();
//        countryDTO.setName("Test Country");
//        countryDTO.setCountryCode("123");
//        countryDTO.setIsoCode("TC");
//
//        country = new Country();
//        country.setCountryId(1L);
//        country.setName("Test Country");
//        country.setCountryCode("+123");
//        country.setIsoCode("TC");
//    }
//
//    @Test
//    void getAllCountries_Success() {
//        when(countryRepository.findAll()).thenReturn(List.of(country));
//        when(countryMapper.toDto(country)).thenReturn(countryDTO);
//
//        List<CountryDTO> result = countryService.getAllCountries();
//
//        assertEquals(1, result.size());
//        assertEquals("Test Country", result.get(0).getName());
//    }
//
//    @Test
//    void getCountryById_Success() {
//        when(countryRepository.findById(1L)).thenReturn(Optional.of(country));
//        when(countryMapper.toDto(country)).thenReturn(countryDTO);
//
//        CountryDTO result = countryService.getCountryById(1L);
//
//        assertNotNull(result);
//        assertEquals("Test Country", result.getName());
//    }
//
//    @Test
//    void getCountryById_NotFound_ThrowsException() {
//        when(countryRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> countryService.getCountryById(1L));
//    }
//
//    @Test
//    void createCountry_Success() {
//        // Arrange
//        CountryDTO inputDTO = new CountryDTO();
//        inputDTO.setName("Test Country");
//        inputDTO.setCountryCode("123");  // Input without +
//
//        Country savedCountry = new Country();
//        savedCountry.setCountryId(1L);
//        savedCountry.setName("Test Country");
//        savedCountry.setCountryCode("123");  // Service removes + during processing
//
//        CountryDTO responseDTO = new CountryDTO();
//        responseDTO.setCountryId(1L);
//        responseDTO.setName("Test Country");
//        responseDTO.setCountryCode("123");
//
//        when(countryRepository.existsByNameAndIsDeletedFalse("Test Country")).thenReturn(false);
//        when(countzryRepository.findByCountryCode("123")).thenReturn(List.of());  // Changed from +123
//        when(countryMapper.toEntity(inputDTO)).thenReturn(savedCountry);
//        when(countryRepository.save(any(Country.class))).thenReturn(savedCountry);
//        when(countryMapper.toDto(savedCountry)).thenReturn(responseDTO);
//
//        // Act
//        CountryResponseDTO result = countryService.createCountry(inputDTO);
//
//        // Assert
//        assertTrue(result.isSuccess());
//        assertEquals("Country created successfully", result.getMessage());
//        assertEquals("Test Country", result.getData().getName());
//        assertEquals("123", result.getData().getCountryCode());
//
//        // Verify interactions
//        verify(countryRepository).existsByNameAndIsDeletedFalse("Test Country");
//        verify(countryRepository).findByCountryCode("123");
//        verify(countryMapper).toEntity(inputDTO);
//        verify(countryRepository).save(any(Country.class));
//        verify(countryMapper).toDto(savedCountry);
//    }
//
//    /**
//     * Tests that country codes with extra spaces are trimmed correctly.
//     */
//    @Test
//    void initializeCountryCodes_TrimsCountryCodes() {
//        // Arrange
//        Country country1 = new Country();
//        country1.setCountryId(1L);
//        country1.setName("Country1");
//        country1.setCountryCode(" +123 ");
//
//        Country country2 = new Country();
//        country2.setCountryId(2L);
//        country2.setName("Country2");
//        country2.setCountryCode(" +456 ");
//
//        List<Country> countries = List.of(country1, country2);
//        when(countryRepository.findAll()).thenReturn(countries);
//
//        // Act
//        countryService.initializeCountryCodes();
//
//        // Assert
//        ArgumentCaptor<Country> captor = ArgumentCaptor.forClass(Country.class);
//        verify(countryRepository, times(2)).save(captor.capture());
//
//        List<Country> savedCountries = captor.getAllValues();
//        assertEquals("+123", savedCountries.get(0).getCountryCode());
//        assertEquals("+456", savedCountries.get(1).getCountryCode());
//    }
//
//    @Test
//    void initializeCountryCodes_NoCountries_DoesNothing() {
//        // Arrange
//        when(countryRepository.findAll()).thenReturn(List.of());
//
//        // Act
//        countryService.initializeCountryCodes();
//
//        // Assert
//        verify(countryRepository, never()).save(any(Country.class));
//    }
//
//    @Test
//    void initializeCountryCodes_CountryCodeWithoutPlus() {
//        // Arrange
//        Country country = new Country();
//        country.setCountryId(1L);
//        country.setName("Country");
//        country.setCountryCode("123 ");
//
//        when(countryRepository.findAll()).thenReturn(List.of(country));
//
//        // Act
//        countryService.initializeCountryCodes();
//
//        // Assert
//        ArgumentCaptor<Country> captor = ArgumentCaptor.forClass(Country.class);
//        verify(countryRepository).save(captor.capture());
//
//        Country savedCountry = captor.getValue();
//        assertEquals("123", savedCountry.getCountryCode()); // trimmed, but no "+"
//    }
//
//
//    @Test
//    void createCountry_DuplicateName_ThrowsException() {
//        when(countryRepository.existsByNameAndIsDeletedFalse("Test Country")).thenReturn(true);
//
//        assertThrows(IllegalArgumentException.class, () -> countryService.createCountry(countryDTO));
//    }
//
//    @Test
//    void createCountry_DuplicateCountryCode_ThrowsException() {
//        // Arrange
//        CountryDTO inputDTO = new CountryDTO();
//        inputDTO.setName("Test Country");
//        inputDTO.setCountryCode("123");
//
//        Country existingCountry = new Country();
//        existingCountry.setCountryId(2L);
//        existingCountry.setName("Other Country");
//        existingCountry.setCountryCode("123");
//
//        // Mock repository behavior - match exact strings that will be used
//        when(countryRepository.existsByNameAndIsDeletedFalse("Test Country")).thenReturn(false);
//        when(countryRepository.findByCountryCode("123")).thenReturn(List.of(existingCountry));
//
//        // Act & Assert
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> countryService.createCountry(inputDTO));
//
//        assertEquals("Country code 123 is already used by countries: Other Country", exception.getMessage());
//
//        // Verify interactions
//        verify(countryRepository).existsByNameAndIsDeletedFalse("Test Country");
//        verify(countryRepository).findByCountryCode("123");
//        verify(countryRepository, never()).save(any(Country.class));
//        verify(countryMapper, never()).toDto(any(Country.class));
//    }
//
//    @Test
//    void createCountry_InvalidCountryCode_ThrowsException() {
//        countryDTO.setCountryCode("invalid");
//
//        assertThrows(IllegalArgumentException.class, () -> countryService.createCountry(countryDTO));
//    }
//    @Test
//    void updateCountry_Success() {
//        // Arrange
//        // Change the DTO name to force a duplicate check
//        countryDTO.setName("New Name");
//
//        when(countryRepository.findById(1L)).thenReturn(Optional.of(country));
//        when(countryRepository.existsByNameAndIsDeletedFalse("New Name")).thenReturn(false); // Match the new name
//        when(countryRepository.save(any(Country.class))).thenReturn(country);
//        when(countryMapper.toDto(country)).thenReturn(countryDTO);
//
//        // Act
//        CountryResponseDTO result = countryService.updateCountry(1L, countryDTO);
//
//        // Assert
//        assertTrue(result.isSuccess());
//    }
//
//    @Test
//    void updateCountry_DuplicateName_ThrowsException() {
//        Country existingCountry = new Country();
//        existingCountry.setCountryId(1L);
//        existingCountry.setName("Old Name");
//        existingCountry.setCountryCode("+123");
//
//        when(countryRepository.findById(1L)).thenReturn(Optional.of(existingCountry));
//        when(countryRepository.existsByNameAndIsDeletedFalse("Test Country")).thenReturn(true);
//
//        assertThrows(IllegalArgumentException.class, () -> countryService.updateCountry(1L, countryDTO));
//    }
//    @Test
//    void updateCountry_DuplicateCountryCode_ThrowsException() {
//        // Arrange
//        Country existingCountry = new Country();
//        existingCountry.setCountryId(1L);
//        existingCountry.setName("Existing Country");
//        existingCountry.setCountryCode("456"); // Different code to trigger duplicate check
//
//        Country anotherCountry = new Country();
//        anotherCountry.setCountryId(2L);
//        anotherCountry.setName("Another Country");
//        anotherCountry.setCountryCode("123"); // Same code as DTO after normalization
//
//        CountryDTO countryDTO = new CountryDTO();
//        countryDTO.setName("Test Country");
//        countryDTO.setCountryCode("123"); // Normalized to +123 by ensureCountryCodeFormat
//
//        when(countryRepository.findById(1L)).thenReturn(Optional.of(existingCountry));
//        when(countryRepository.existsByNameAndIsDeletedFalse("Test Country")).thenReturn(false);
//        when(countryRepository.findByCountryCode("123")).thenReturn(List.of(anotherCountry));
//
//        // Act & Assert
//        assertThrows(IllegalArgumentException.class, () -> countryService.updateCountry(1L, countryDTO));
//
//        // Verify interactions
//        verify(countryRepository, times(1)).findByCountryCode("123");
//        verify(countryRepository).existsByNameAndIsDeletedFalse("Test Country");
//        verify(countryRepository).findById(1L);
//    }
//
//    @Test
//    void updateCountry_NotFound_ThrowsException() {
//        when(countryRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> countryService.updateCountry(1L, countryDTO));
//    }
//
//    @Test
//    void updateCountry_InvalidCountryCode_ThrowsException() {
//        Country existingCountry = new Country();
//        existingCountry.setCountryId(1L);
//        existingCountry.setName("Existing Country");
//        existingCountry.setCountryCode("+123");
//
//        CountryDTO dto = new CountryDTO();
//        dto.setName("Existing Country");
//        dto.setCountryCode("invalid");
//
//        when(countryRepository.findById(1L)).thenReturn(Optional.of(existingCountry));
//
//        assertThrows(IllegalArgumentException.class, () -> countryService.updateCountry(1L, dto));
//    }
//
//    @Test
//    void createCountry_NullCountryCode_ThrowsException() {
//        countryDTO.setCountryCode(null);
//        assertThrows(IllegalArgumentException.class, () -> countryService.createCountry(countryDTO));
//    }
//
//    @Test
//    void updateCountry_SameNameAndCode_Success() {
//        Country existingCountry = new Country();
//        existingCountry.setCountryId(1L);
//        existingCountry.setName("Test Country");
//        existingCountry.setCountryCode("+123");
//
//        CountryDTO dto = new CountryDTO();
//        dto.setName("Test Country");
//        dto.setCountryCode("+123");
//
//        when(countryRepository.findById(1L)).thenReturn(Optional.of(existingCountry));
//        when(countryRepository.save(any(Country.class))).thenReturn(existingCountry);
//        when(countryMapper.toDto(existingCountry)).thenReturn(dto);
//
//        CountryResponseDTO result = countryService.updateCountry(1L, dto);
//        assertTrue(result.isSuccess());
//        assertEquals("Test Country", result.getData().getName());
//    }
//
//    @Test
//    void createCountry_CountryCodeWithSpacesAndPlus() {
//        countryDTO.setCountryCode(" +123 ");
//        when(countryRepository.existsByNameAndIsDeletedFalse("Test Country")).thenReturn(false);
//        when(countryRepository.findByCountryCode("123")).thenReturn(List.of());
//        when(countryMapper.toEntity(countryDTO)).thenReturn(country);
//        when(countryRepository.save(any(Country.class))).thenReturn(country);
//        when(countryMapper.toDto(country)).thenReturn(countryDTO);
//
//        CountryResponseDTO result = countryService.createCountry(countryDTO);
//        assertTrue(result.isSuccess());
//        assertEquals("Test Country", result.getData().getName());
//    }
//
//    @Test
//    void deleteCountry_Success() {
//        // Arrange
//        when(countryRepository.existsById(1L)).thenReturn(true);
//        when(prefixRepository.existsByCountry_CountryIdAndIsDeletedFalse(1L)).thenReturn(false); // Mock check
//        doNothing().when(countryRepository).deleteById(1L);
//
//        // Act
//        countryService.deleteCountry(1L);
//
//        // Assert
//        verify(countryRepository).deleteById(1L);
//        verify(prefixRepository).existsByCountry_CountryIdAndIsDeletedFalse(1L); // Verify interaction
//    }
//
//    @Test
//    void deleteCountry_NotFound_ThrowsException() {
//        when(countryRepository.existsById(1L)).thenReturn(false);
//
//        assertThrows(ResourceNotFoundException.class, () -> countryService.deleteCountry(1L));
//    }
//
//    @Test
//    void deleteCountry_ReferencedByPrefix_ThrowsException() {
//        when(countryRepository.existsById(1L)).thenReturn(true);
//        when(prefixRepository.existsByCountry_CountryIdAndIsDeletedFalse(1L)).thenReturn(true);
//
//        assertThrows(ForeignReferenceException.class, () -> countryService.deleteCountry(1L));
//    }
//
//    @Test
//    void searchCountries() {
//        // Arrange
//        CountrySearchDTO searchDTO = new CountrySearchDTO();
//        searchDTO.setSearchTerm("Ind");
//
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Country country1 = new Country();
//        country1.setCountryId(1L);
//        country1.setName("India");
//        country1.setCountryCode("+91");
//
//        Country country2 = new Country();
//        country2.setCountryId(2L);
//        country2.setName("Indonesia");
//        country2.setCountryCode("+62");
//
//        CountryDTO countryDTO1 = new CountryDTO();
//        countryDTO1.setCountryId(1L);
//        countryDTO1.setName("India");
//        countryDTO1.setCountryCode("+91");
//
//        CountryDTO countryDTO2 = new CountryDTO();
//        countryDTO2.setCountryId(2L);
//        countryDTO2.setName("Indonesia");
//        countryDTO2.setCountryCode("+62");
//
//        Page<Country> countryPage = new PageImpl<>(
//            List.of(country1, country2),
//            pageable,
//            2
//        );
//
//        // Mock repository and mapper behavior
//        when(countryRepository.searchCountries("Ind", pageable))
//            .thenReturn(countryPage);
//        when(countryMapper.toDto(country1)).thenReturn(countryDTO1);
//        when(countryMapper.toDto(country2)).thenReturn(countryDTO2);
//
//        // Act
//        PageResponseDTO<CountryDTO> result = countryService.searchCountries(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(2, result.getContent().size());
//
//        // Verify first country
//        assertEquals("India", result.getContent().get(0).getName());
//        assertEquals("+91", result.getContent().get(0).getCountryCode());
//
//        // Verify second country
//        assertEquals("Indonesia", result.getContent().get(1).getName());
//        assertEquals("+62", result.getContent().get(1).getCountryCode());
//
//        // Verify repository was called with correct parameters
//        verify(countryRepository).searchCountries("Ind", pageable);
//
//        // Verify mapper was called for each country
//        verify(countryMapper).toDto(country1);
//        verify(countryMapper).toDto(country2);
//    }
//
//    @Test
//    void searchCountries_NoResults() {
//        // Arrange
//        CountrySearchDTO searchDTO = new CountrySearchDTO();
//        searchDTO.setSearchTerm("XYZ");
//
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Page<Country> emptyPage = new PageImpl<>(
//            List.of(),
//            pageable,
//            0
//        );
//
//        when(countryRepository.searchCountries("XYZ", pageable))
//            .thenReturn(emptyPage);
//
//        // Act
//        PageResponseDTO<CountryDTO> result = countryService.searchCountries(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        // Verify repository was called with correct parameters
//        verify(countryRepository).searchCountries("XYZ", pageable);
//
//        // Verify mapper was never called since there were no results
//        verifyNoInteractions(countryMapper);
//    }
//
//    @Test
//    void searchCountries_NullSearchTerm() {
//        // Arrange
//        CountrySearchDTO searchDTO = new CountrySearchDTO();
//        searchDTO.setSearchTerm(null);
//
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Country country = new Country();
//        country.setCountryId(1L);
//        country.setName("India");
//        country.setCountryCode("+91");
//
//        CountryDTO countryDTO = new CountryDTO();
//        countryDTO.setCountryId(1L);
//        countryDTO.setName("India");
//        countryDTO.setCountryCode("+91");
//
//        Page<Country> countryPage = new PageImpl<>(
//            List.of(country),
//            pageable,
//            1
//        );
//
//        when(countryRepository.searchCountries(null, pageable))
//            .thenReturn(countryPage);
//        when(countryMapper.toDto(country)).thenReturn(countryDTO);
//
//        // Act
//        PageResponseDTO<CountryDTO> result = countryService.searchCountries(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(1, result.getContent().size());
//
//        // Verify repository was called with null search term
//        verify(countryRepository).searchCountries(null, pageable);
//
//        // Verify mapper was called once
//        verify(countryMapper).toDto(country);
//    }
//
//    @Test
//    void getCountriesInPage_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Create second country
//        Country country2 = new Country();
//        country2.setCountryId(2L);
//        country2.setName("Second Country");
//        country2.setCountryCode("456");
//
//        // Create DTO for second country
//        CountryDTO countryDTO2 = new CountryDTO();
//        countryDTO2.setCountryId(2L);
//        countryDTO2.setName("Second Country");
//        countryDTO2.setCountryCode("456");
//
//        // Use existing country from setUp()
//        Page<Country> countryPage = new PageImpl<>(
//            List.of(country, country2),
//            pageable,
//            2
//        );
//
//        // Mock behavior using setUp() objects
//        when(countryRepository.findAll(pageable)).thenReturn(countryPage);
//        when(countryMapper.toDto(country)).thenReturn(countryDTO);
//        when(countryMapper.toDto(country2)).thenReturn(countryDTO2);
//
//        // Act
//        PageResponseDTO<CountryDTO> result = countryService.getCountriesInPage(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(2, result.getContent().size());
//
//        // Verify first country (from setUp())
//        assertEquals("Test Country", result.getContent().get(0).getName());
//        assertEquals("123", result.getContent().get(0).getCountryCode());
//
//        // Verify second country
//        assertEquals("Second Country", result.getContent().get(1).getName());
//        assertEquals("456", result.getContent().get(1).getCountryCode());
//
//        // Verify interactions
//        verify(countryRepository).findAll(pageable);
//        verify(countryMapper, times(2)).toDto(any(Country.class));
//    }
//
//    @Test
//    void getCountriesInPage_EmptyPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Country> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//
//        when(countryRepository.findAll(pageable)).thenReturn(emptyPage);
//
//        // Act
//        PageResponseDTO<CountryDTO> result = countryService.getCountriesInPage(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        // Verify interactions
//        verify(countryRepository).findAll(pageable);
//        verifyNoInteractions(countryMapper);
//    }
//
//    @Test
//    void getCountriesInPage_MultiplePages() {
//        // Arrange
//        int pageSize = 1;
//        Pageable pageable = PageRequest.of(0, pageSize);
//
//        // Use country from setUp() for first page
//        Page<Country> firstPage = new PageImpl<>(
//            List.of(country),
//            pageable,
//            2 // total elements
//        );
//
//        when(countryRepository.findAll(pageable)).thenReturn(firstPage);
//        when(countryMapper.toDto(country)).thenReturn(countryDTO);
//
//        // Act
//        PageResponseDTO<CountryDTO> result = countryService.getCountriesInPage(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(2, result.getPageDetails().getTotalPages());// 2 records with page size 1 = 2 pages
//        assertEquals(1, result.getContent().size());
//
//        // Verify content using setUp() objects
//        CountryDTO firstCountry = result.getContent().get(0);
//        assertEquals(countryDTO.getName(), firstCountry.getName());
//        assertEquals(countryDTO.getCountryCode(), firstCountry.getCountryCode());
//
//        // Verify interactions
//        verify(countryRepository).findAll(pageable);
//        verify(countryMapper).toDto(country);
//    }
//
//    @Test
//    void getCountriesInPage_LastPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(1, 1); // Second page with size 1
//
//        // Use country from setUp()
//        Page<Country> lastPage = new PageImpl<>(
//            List.of(country),
//            pageable,
//            2 // total elements
//        );
//
//        when(countryRepository.findAll(pageable)).thenReturn(lastPage);
//        when(countryMapper.toDto(country)).thenReturn(countryDTO);
//
//        // Act
//        PageResponseDTO<CountryDTO> result = countryService.getCountriesInPage(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(2, result.getPageDetails().getTotalPages());
//        assertEquals(1, result.getContent().size());
//        assertEquals(countryDTO.getName(), result.getContent().get(0).getName());
//
//        // Verify interactions
//        verify(countryRepository).findAll(pageable);
//        verify(countryMapper).toDto(any(Country.class));
//    }
//}
