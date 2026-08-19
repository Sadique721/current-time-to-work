//package com.xcess.ocs.service;
//
//import com.xcess.ocs.dto.PageResponseDTO;
//import com.xcess.ocs.dto.PartnerDTO;
//import com.xcess.ocs.dto.search.PartnerSearchDTO;
//import com.xcess.ocs.entity.Partner;
//import com.xcess.ocs.exception.DuplicatePartnerException;
//import com.xcess.ocs.exception.ForeignReferenceException;
//import com.xcess.ocs.exception.ResourceNotFoundException;
//import com.xcess.ocs.mapper.PartnerMapper;
//import com.xcess.ocs.repository.AccountRepository;
//import com.xcess.ocs.repository.PartnerRepository;
//import com.xcess.ocs.util.PaginationUtils;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PartnerServiceTest {
//
//    @Mock
//    private PartnerRepository partnerRepository;
//
//    @Mock
//    private AccountRepository accountRepository;
//
//    @Mock
//    private PartnerMapper partnerMapper;
//
//    @InjectMocks
//    private PartnerService partnerService;
//
//    private PartnerDTO partnerDTO;
//    private Partner partner;
//
//    @BeforeEach
//    void setUp() {
//        partnerDTO = new PartnerDTO();
//        partnerDTO.setPartnerName("Test Partner");
//        partnerDTO.setPartnerType("CUSTOMER");
//
//        partner = new Partner();
//        partner.setPartnerId(1L);
//        partner.setPartnerName("Test Partner");
//        partner.setPartnerType("CUSTOMER");
//    }
//
//    @Test
//    void createPartner_Success() {
//        when(partnerRepository.existsByPartnerNameAndIsDeletedFalse("Test Partner")).thenReturn(false);
//        when(partnerMapper.toEntity(partnerDTO)).thenReturn(partner);
//        when(partnerRepository.save(any(Partner.class))).thenReturn(partner);
//        when(partnerMapper.toDto(partner)).thenReturn(partnerDTO);
//
//        PartnerDTO createdPartner = partnerService.createPartner(partnerDTO);
//
//        assertNotNull(createdPartner);
//        assertEquals("Test Partner", createdPartner.getPartnerName());
//        verify(partnerRepository).existsByPartnerNameAndIsDeletedFalse("Test Partner");
//        verify(partnerRepository).save(any(Partner.class));
//        verify(partnerMapper).toEntity(partnerDTO);
//        verify(partnerMapper).toDto(partner);
//    }
//
//    @Test
//    void createPartner_DuplicateName_ThrowsException() {
//        when(partnerRepository.existsByPartnerNameAndIsDeletedFalse("Test Partner")).thenReturn(true);
//
//        Exception ex = assertThrows(DuplicatePartnerException.class, () -> partnerService.createPartner(partnerDTO));
//
//        assertTrue(ex.getMessage().contains("Partner name already exists"));
//        verify(partnerRepository).existsByPartnerNameAndIsDeletedFalse("Test Partner");
//        verify(partnerRepository, never()).save(any(Partner.class));
//    }
//
//    @Test
//    void createPartner_InvalidPartnerType_ThrowsException() {
//        partnerDTO.setPartnerType("INVALID_TYPE");
//
//        Exception ex = assertThrows(IllegalArgumentException.class, () -> partnerService.createPartner(partnerDTO));
//
//        assertTrue(ex.getMessage().contains("Partner type must be either"));
//        verify(partnerRepository, never()).save(any(Partner.class));
//    }
//
//    @Test
//    void getAllPartners_Success() {
//        List<Partner> partners = Arrays.asList(partner, new Partner());
//        when(partnerRepository.findAll()).thenReturn(partners);
//        when(partnerMapper.toDto(any(Partner.class))).thenReturn(partnerDTO);
//
//        List<PartnerDTO> result = partnerService.getAllPartners();
//
//        assertEquals(2, result.size());
//        verify(partnerRepository).findAll();
//        verify(partnerMapper, times(2)).toDto(any(Partner.class));
//    }
//
//    @Test
//    void getPartnerById_Success() {
//        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
//        when(partnerMapper.toDto(partner)).thenReturn(partnerDTO);
//
//        PartnerDTO result = partnerService.getPartnerById(1L);
//
//        assertNotNull(result);
//        assertEquals("Test Partner", result.getPartnerName());
//        verify(partnerRepository).findById(1L);
//        verify(partnerMapper).toDto(partner);
//    }
//
//    @Test
//    void getPartnerById_NotFound_ThrowsException() {
//        when(partnerRepository.findById(1L)).thenReturn(Optional.empty());
//
//        Exception ex = assertThrows(ResourceNotFoundException.class, () -> partnerService.getPartnerById(1L));
//
//        assertTrue(ex.getMessage().contains("Partner not found"));
//        verify(partnerRepository).findById(1L);
//    }
//
//    @Test
//    void updatePartner_Success() {
//        PartnerDTO updateDTO = new PartnerDTO();
//        updateDTO.setPartnerName("Updated Partner");
//        updateDTO.setPartnerType("VENDOR");
//
//        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
//        when(partnerRepository.existsByPartnerNameAndIsDeletedFalse("Updated Partner")).thenReturn(false);
//        when(partnerRepository.save(any(Partner.class))).thenReturn(partner);
//        when(partnerMapper.toDto(partner)).thenReturn(updateDTO);
//
//        PartnerDTO result = partnerService.updatePartner(1L, updateDTO);
//
//        assertNotNull(result);
//        assertEquals("Updated Partner", result.getPartnerName());
//        verify(partnerRepository).findById(1L);
//        verify(partnerRepository).save(any(Partner.class));
//    }
//
//    @Test
//    void updatePartner_InvalidPartnerType_ThrowsException() {
//        PartnerDTO updateDTO = new PartnerDTO();
//        updateDTO.setPartnerType("INVALID");
//
//        Exception ex = assertThrows(IllegalArgumentException.class, () -> partnerService.updatePartner(1L, updateDTO));
//
//        assertTrue(ex.getMessage().contains("Partner type must be either"));
//        verify(partnerRepository, never()).save(any(Partner.class));
//    }
//
//    @Test
//    void deletePartner_Success() {
//        when(partnerRepository.existsById(1L)).thenReturn(true);
//        when(accountRepository.existsByPartner_PartnerIdAndIsDeletedFalse(1L)).thenReturn(false);
//
//        partnerService.deletePartner(1L);
//
//        verify(partnerRepository).existsById(1L);
//        verify(partnerRepository).deleteById(1L);
//    }
//
//    @Test
//    void deletePartner_NotFound_ThrowsException() {
//        when(partnerRepository.existsById(1L)).thenReturn(false);
//
//        Exception ex = assertThrows(ResourceNotFoundException.class, () -> partnerService.deletePartner(1L));
//
//        assertTrue(ex.getMessage().contains("Partner not found"));
//        verify(partnerRepository, never()).deleteById(anyLong());
//    }
//
//    @Test
//    void deletePartner_ReferencedByActiveAccount_ThrowsException() {
//        when(partnerRepository.existsById(1L)).thenReturn(true);
//        when(accountRepository.existsByPartner_PartnerIdAndIsDeletedFalse(1L)).thenReturn(true);
//
//        Exception ex = assertThrows(ForeignReferenceException.class, () -> partnerService.deletePartner(1L));
//
//        assertTrue(ex.getMessage().contains("referenced by an active account"));
//        verify(partnerRepository, never()).deleteById(anyLong());
//    }
//
//    @Test
//    void getPartnersInPage_Success() {
//        Pageable pageable = mock(Pageable.class);
//        Page<Partner> partnerPage = mock(Page.class);
//        List<Partner> partners = Arrays.asList(partner);
//        List<PartnerDTO> partnerDTOs = Arrays.asList(partnerDTO);
//        PageResponseDTO<PartnerDTO> responseDTO = mock(PageResponseDTO.class);
//
//        when(partnerRepository.findAll(pageable)).thenReturn(partnerPage);
//        when(partnerPage.getContent()).thenReturn(partners);
//        when(partnerMapper.toDto(partner)).thenReturn(partnerDTO);
//
//        try (MockedStatic<PaginationUtils> utilities = mockStatic(PaginationUtils.class)) {
//            utilities.when(() -> PaginationUtils.buildGetResponseDTO(partnerDTOs, partnerPage)).thenReturn(responseDTO);
//
//            PageResponseDTO<PartnerDTO> result = partnerService.getPartnersInPage(pageable);
//
//            assertEquals(responseDTO, result);
//        }
//
//        verify(partnerRepository).findAll(pageable);
//        verify(partnerMapper).toDto(partner);
//    }
//
//    @Test
//    void searchPartners_Success() {
//        PartnerSearchDTO searchDTO = new PartnerSearchDTO();
//        Pageable pageable = mock(Pageable.class);
//        Page<Partner> partnerPage = mock(Page.class);
//        List<Partner> partners = Arrays.asList(partner);
//        List<PartnerDTO> partnerDTOs = Arrays.asList(partnerDTO);
//        PageResponseDTO<PartnerDTO> responseDTO = mock(PageResponseDTO.class);
//
//        when(partnerRepository.searchPartners(any(), any(), eq(pageable))).thenReturn(partnerPage);
//        when(partnerPage.getContent()).thenReturn(partners);
//        when(partnerMapper.toDto(partner)).thenReturn(partnerDTO);
//
//        try (MockedStatic<PaginationUtils> utilities = mockStatic(PaginationUtils.class)) {
//            utilities.when(() -> PaginationUtils.buildGetResponseDTO(partnerDTOs, partnerPage)).thenReturn(responseDTO);
//
//            PageResponseDTO<PartnerDTO> result = partnerService.searchPartners(searchDTO, pageable);
//
//            assertEquals(responseDTO, result);
//        }
//
//        verify(partnerRepository).searchPartners(any(), any(), eq(pageable));
//        verify(partnerMapper).toDto(partner);
//    }
//
//    @Test
//    void getPartnersInPage_EmptyPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Partner> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//        PageResponseDTO<PartnerDTO> emptyResponseDTO = new PageResponseDTO<>();
//
//        when(partnerRepository.findAll(pageable)).thenReturn(emptyPage);
//
//        try (MockedStatic<PaginationUtils> utilities = mockStatic(PaginationUtils.class)) {
//            utilities.when(() -> PaginationUtils.buildGetResponseDTO(List.of(), emptyPage))
//                    .thenReturn(emptyResponseDTO);
//
//            // Act
//            PageResponseDTO<PartnerDTO> result = partnerService.getPartnersInPage(pageable);
//
//            // Assert
//            assertNotNull(result);
//            assertEquals(emptyResponseDTO, result);
//            verify(partnerRepository).findAll(pageable);
//            verifyNoInteractions(partnerMapper);
//        }
//    }
//
//    @Test
//    void searchPartners_NullSearchDTO() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Partner> partnerPage = new PageImpl<>(List.of(partner), pageable, 1);
//
//        when(partnerRepository.searchPartners(null, null, pageable))
//                .thenReturn(partnerPage);
//        when(partnerMapper.toDto(partner)).thenReturn(partnerDTO);
//
//        // Act
//        PageResponseDTO<PartnerDTO> result = partnerService.searchPartners(null, pageable);
//
//        // Assert
//        assertNotNull(result);
//        verify(partnerRepository).searchPartners(null, null, pageable);
//        verify(partnerMapper).toDto(partner);
//    }
//
//    @Test
//    void searchPartners_EmptyResults() {
//        // Arrange
//        PartnerSearchDTO searchDTO = new PartnerSearchDTO();
//        searchDTO.setPartnerName("NonExistent");
//        searchDTO.setPartnerType("CUSTOMER");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Page<Partner> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//        PageResponseDTO<PartnerDTO> emptyResponseDTO = new PageResponseDTO<>();
//
//        when(partnerRepository.searchPartners("NonExistent", "CUSTOMER", pageable))
//                .thenReturn(emptyPage);
//
//        try (MockedStatic<PaginationUtils> utilities = mockStatic(PaginationUtils.class)) {
//            utilities.when(() -> PaginationUtils.buildGetResponseDTO(List.of(), emptyPage))
//                    .thenReturn(emptyResponseDTO);
//
//            // Act
//            PageResponseDTO<PartnerDTO> result = partnerService.searchPartners(searchDTO, pageable);
//
//            // Assert
//            assertNotNull(result);
//            assertEquals(emptyResponseDTO, result);
//            verify(partnerRepository).searchPartners("NonExistent", "CUSTOMER", pageable);
//            verifyNoInteractions(partnerMapper);
//        }
//    }
//
//    @Test
//    void searchPartners_OnlyPartnerName() {
//        // Arrange
//        PartnerSearchDTO searchDTO = new PartnerSearchDTO();
//        searchDTO.setPartnerName("Test");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use partner from setUp()
//        Page<Partner> partnerPage = new PageImpl<>(List.of(partner), pageable, 1);
//
//        when(partnerRepository.searchPartners("Test", null, pageable))
//                .thenReturn(partnerPage);
//        when(partnerMapper.toDto(partner)).thenReturn(partnerDTO);
//
//        // Act
//        PageResponseDTO<PartnerDTO> result = partnerService.searchPartners(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        verify(partnerRepository).searchPartners("Test", null, pageable);
//        verify(partnerMapper).toDto(partner);
//    }
//
//    @Test
//    void searchPartners_OnlyPartnerType() {
//        // Arrange
//        PartnerSearchDTO searchDTO = new PartnerSearchDTO();
//        searchDTO.setPartnerType("VENDOR");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Partner vendorPartner = new Partner();
//        vendorPartner.setPartnerId(2L);
//        vendorPartner.setPartnerName("Vendor Partner");
//        vendorPartner.setPartnerType("VENDOR");
//
//        PartnerDTO vendorDTO = new PartnerDTO();
//        vendorDTO.setPartnerId(2L);
//        vendorDTO.setPartnerName("Vendor Partner");
//        vendorDTO.setPartnerType("VENDOR");
//
//        Page<Partner> partnerPage = new PageImpl<>(List.of(vendorPartner), pageable, 1);
//
//        when(partnerRepository.searchPartners(null, "VENDOR", pageable))
//                .thenReturn(partnerPage);
//        when(partnerMapper.toDto(vendorPartner)).thenReturn(vendorDTO);
//
//        // Act
//        PageResponseDTO<PartnerDTO> result = partnerService.searchPartners(searchDTO, pageable);
//        // Assert
//        assertNotNull(result);
//        verify(partnerRepository).searchPartners(null, "VENDOR", pageable);
//        verify(partnerMapper).toDto(vendorPartner);
//    }
//}
