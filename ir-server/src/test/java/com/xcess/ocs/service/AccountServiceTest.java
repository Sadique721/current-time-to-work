//package com.xcess.ocs.service;
//
//import com.xcess.ocs.dto.AccountDTO;
//import com.xcess.ocs.dto.PageResponseDTO;
//import com.xcess.ocs.dto.search.AccountSearchDTO;
//import com.xcess.ocs.entity.Account;
//import com.xcess.ocs.entity.Partner;
//import com.xcess.ocs.entity.ProductPlan;
//import com.xcess.ocs.exception.DuplicateAccountCodeException;
//import com.xcess.ocs.exception.ResourceNotFoundException;
//import com.xcess.ocs.repository.AccountRepository;
//import com.xcess.ocs.repository.PartnerRepository;
//import com.xcess.ocs.repository.ProductPlanRepository;
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
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AccountServiceTest {
//
//    @Mock
//    private AccountRepository accountRepository;
//
//    @Mock
//    private PartnerRepository partnerRepository;
//
//    @Mock
//    private ProductPlanRepository productPlanRepository;
//
//    @InjectMocks
//    private AccountService accountService;
//
//    private AccountDTO accountDTO;
//    private Account account;
//    private Partner partner;
//    private ProductPlan productPlan;
//
//    @BeforeEach
//    void setUp() {
//        accountDTO = new AccountDTO();
//        accountDTO.setAccountId(1L);
//        accountDTO.setAccountCode("ACC123");
//        accountDTO.setPartnerId(1L);
//        accountDTO.setProductPlanId(1L);
//        accountDTO.setPartnerType("CUSTOMER");
//
//        partner = new Partner();
//        partner.setPartnerId(1L);
//        partner.setPartnerType("CUSTOMER");
//
//        productPlan = new ProductPlan();
//        productPlan.setProductPlanId(1L);
//
//        account = new Account();
//        account.setAccountId(1L);
//        account.setAccountCode("ACC123");
//        account.setPartner(partner);
//        account.setProductPlan(productPlan);
//        account.setPartnerType("CUSTOMER");
//    }
//
//    @Test
//    void createAccount_Success() {
//        when(accountRepository.existsByAccountCodeAndIsDeletedFalse("ACC123")).thenReturn(false);
//        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(productPlan));
//        when(accountRepository.save(any(Account.class))).thenReturn(account);
//
//        AccountDTO result = accountService.createAccount(accountDTO);
//
//        assertNotNull(result);
//        assertEquals("ACC123", result.getAccountCode());
//        assertEquals(1L, result.getPartnerId());
//        verify(accountRepository).save(any(Account.class));
//    }
//
//    @Test
//    void createAccount_DuplicateAccountCode_ThrowsException() {
//        when(accountRepository.existsByAccountCodeAndIsDeletedFalse("ACC123")).thenReturn(true);
//        assertThrows(DuplicateAccountCodeException.class, () -> accountService.createAccount(accountDTO));
//        verify(accountRepository, never()).save(any(Account.class));
//    }
//
//    @Test
//    void createAccount_PartnerNotFound_ThrowsException() {
//        when(accountRepository.existsByAccountCodeAndIsDeletedFalse("ACC123")).thenReturn(false);
//        when(partnerRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> accountService.createAccount(accountDTO));
//        verify(accountRepository, never()).save(any(Account.class));
//    }
//
//    @Test
//    void createAccount_ProductPlanNotFound_ThrowsException() {
//        when(accountRepository.existsByAccountCodeAndIsDeletedFalse("ACC123")).thenReturn(false);
//        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> accountService.createAccount(accountDTO));
//    }
//
//    @Test
//    void getAllAccounts_Success() {
//        when(accountRepository.findAll()).thenReturn(List.of(account));
//        List<AccountDTO> result = accountService.getAllAccounts();
//        assertEquals(1, result.size());
//        assertEquals("ACC123", result.get(0).getAccountCode());
//        verify(accountRepository).findAll();
//    }
//
//    @Test
//    void getAllAccounts_Empty() {
//        when(accountRepository.findAll()).thenReturn(Collections.emptyList());
//        List<AccountDTO> result = accountService.getAllAccounts();
//        assertTrue(result.isEmpty());
//        verify(accountRepository).findAll();
//    }
//
//    @Test
//    void getAccountById_Success() {
//        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
//        AccountDTO result = accountService.getAccountById(1L);
//        assertNotNull(result);
//        assertEquals("ACC123", result.getAccountCode());
//        verify(accountRepository).findById(1L);
//    }
//
//    @Test
//    void getAccountById_NotFound() {
//        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountById(1L));
//    }
//
//    @Test
//    void getAccountsByPartnerId_Success() {
//        when(accountRepository.findByPartnerPartnerId(1L)).thenReturn(List.of(account));
//        List<AccountDTO> result = accountService.getAccountsByPartnerId(1L);
//        assertEquals(1, result.size());
//        assertEquals("ACC123", result.get(0).getAccountCode());
//        verify(accountRepository).findByPartnerPartnerId(1L);
//    }
//
//    @Test
//    void getAccountsByPartnerId_NotFound() {
//        when(accountRepository.findByPartnerPartnerId(1L)).thenReturn(Collections.emptyList());
//        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountsByPartnerId(1L));
//    }
//
//    @Test
//    void updateAccount_Success() {
//        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
//        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(productPlan));
//        when(accountRepository.save(any(Account.class))).thenReturn(account);
//        AccountDTO result = accountService.updateAccount(1L, accountDTO);
//        assertNotNull(result);
//        assertEquals("ACC123", result.getAccountCode());
//        verify(accountRepository).save(any(Account.class));
//    }
//
//    @Test
//    void updateAccount_PartnerNotFound() {
//        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
//        when(partnerRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> accountService.updateAccount(1L, accountDTO));
//    }
//
//    @Test
//    void updateAccount_ProductPlanNotFound() {
//        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
//        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> accountService.updateAccount(1L, accountDTO));
//    }
//
//    @Test
//    void updateAccount_DuplicateAccountCode() {
//        accountDTO.setAccountCode("NEW_ACC");
//        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
//        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(productPlan));
//        when(accountRepository.existsByAccountCodeAndIsDeletedFalse("NEW_ACC")).thenReturn(true);
//        assertThrows(DuplicateAccountCodeException.class, () -> accountService.updateAccount(1L, accountDTO));
//        verify(accountRepository, never()).save(any(Account.class));
//    }
//
//    @Test
//    void updateAccount_NotFound() {
//        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> accountService.updateAccount(1L, accountDTO));
//    }
//
//    @Test
//    void deleteAccount_Success() {
//        when(accountRepository.existsById(1L)).thenReturn(true);
//        accountService.deleteAccount(1L);
//        verify(accountRepository).deleteById(1L);
//    }
//
//    @Test
//    void deleteAccount_NotFound() {
//        when(accountRepository.existsById(1L)).thenReturn(false);
//        assertThrows(ResourceNotFoundException.class, () -> accountService.deleteAccount(1L));
//        verify(accountRepository, never()).deleteById(1L);
//    }
//
//    @Test
//    void createAccount_nullDto_throwsException() {
//        assertThrows(NullPointerException.class, () -> accountService.createAccount(null));
//    }
//
//    @Test
//    void updateAccount_nullDto_throwsException() {
//        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
//        assertThrows(NullPointerException.class, () -> accountService.updateAccount(1L, null));
//    }
//
//    @Test
//    void updateAccount_notFound_throwsException() {
//        when(accountRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> accountService.updateAccount(1L, accountDTO));
//    }
//
//    @Test
//    void createAccount_invalidProductPlan_throwsException() {
//        when(accountRepository.existsByAccountCodeAndIsDeletedFalse("ACC123")).thenReturn(false);
//        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> accountService.createAccount(accountDTO));
//    }
//
//    @Test
//    void createAccount_incompatiblePartnerType_throwsException() {
//        accountDTO.setPartnerType("CUSTOMER");
//        partner.setPartnerType("VENDOR");
//        when(accountRepository.existsByAccountCodeAndIsDeletedFalse("ACC123")).thenReturn(false);
//        when(partnerRepository.findById(1L)).thenReturn(Optional.of(partner));
//        when(productPlanRepository.findById(1L)).thenReturn(Optional.of(productPlan));
//        assertThrows(NullPointerException.class, () -> accountService.createAccount(accountDTO));
//    }
//
//    @Test
//    void getAllAccounts_emptyList() {
//        when(accountRepository.findAll()).thenReturn(Collections.emptyList());
//        List<AccountDTO> result = accountService.getAllAccounts();
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void getAccountsInPages_Success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Create second account using existing objects from setUp
//        Account account2 = new Account();
//        account2.setAccountId(2L);
//        account2.setAccountCode("ACC456");
//        account2.setPartner(partner);
//        account2.setProductPlan(productPlan);
//        account2.setPartnerType("CUSTOMER");
//
//        Page<Account> accountPage = new PageImpl<>(
//                List.of(account, account2),
//                pageable,
//                2
//        );
//
//        when(accountRepository.findAll(pageable)).thenReturn(accountPage);
//
//        // Act
//        PageResponseDTO<AccountDTO> result = accountService.getAccountsInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(2, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(2, result.getContent().size());
//
//        // Verify first account (from setUp)
//        assertEquals("ACC123", result.getContent().get(0).getAccountCode());
//        assertEquals("CUSTOMER", result.getContent().get(0).getPartnerType());
//
//        // Verify second account
//        assertEquals("ACC456", result.getContent().get(1).getAccountCode());
//        assertEquals("CUSTOMER", result.getContent().get(1).getPartnerType());
//
//        verify(accountRepository).findAll(pageable);
//    }
//
//    @Test
//    void getAccountsInPages_EmptyPage() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Account> emptyPage = new PageImpl<>(List.of(), pageable, 0);
//
//        when(accountRepository.findAll(pageable)).thenReturn(emptyPage);
//
//        // Act
//        PageResponseDTO<AccountDTO> result = accountService.getAccountsInPages(pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(accountRepository).findAll(pageable);
//    }
//
//    @Test
//    void searchAccounts_WithAllCriteria() {
//        // Arrange
//        AccountSearchDTO searchDTO = new AccountSearchDTO();
//        searchDTO.setSearchTerm("ACC");
//        searchDTO.setPartnerType("CUSTOMER");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use account from setUp
//        Page<Account> searchResults = new PageImpl<>(
//                List.of(account),
//                pageable,
//                1
//        );
//
//        when(accountRepository.searchAccounts("ACC", "CUSTOMER", pageable))
//                .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<AccountDTO> result = accountService.searchAccounts(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getPageDetails().getTotalPages());
//        assertEquals(1, result.getContent().size());
//        assertEquals("ACC123", result.getContent().get(0).getAccountCode());
//        assertEquals("CUSTOMER", result.getContent().get(0).getPartnerType());
//
//        verify(accountRepository).searchAccounts("ACC", "CUSTOMER", pageable);
//    }
//
//    @Test
//    void searchAccounts_OnlySearchTerm() {
//        // Arrange
//        AccountSearchDTO searchDTO = new AccountSearchDTO();
//        searchDTO.setSearchTerm("ACC");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Page<Account> searchResults = new PageImpl<>(
//                List.of(account),
//                pageable,
//                1
//        );
//
//        when(accountRepository.searchAccounts("ACC", null, pageable))
//                .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<AccountDTO> result = accountService.searchAccounts(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals("ACC123", result.getContent().get(0).getAccountCode());
//
//        verify(accountRepository).searchAccounts("ACC", null, pageable);
//    }
//
//    @Test
//    void searchAccounts_OnlyPartnerType() {
//        // Arrange
//        AccountSearchDTO searchDTO = new AccountSearchDTO();
//        searchDTO.setPartnerType("VENDOR");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Account vendorAccount = new Account();
//        vendorAccount.setAccountId(2L);
//        vendorAccount.setAccountCode("VEN123");
//        vendorAccount.setPartner(partner);
//        vendorAccount.setProductPlan(productPlan);
//        vendorAccount.setPartnerType("VENDOR");
//
//        Page<Account> searchResults = new PageImpl<>(
//                List.of(vendorAccount),
//                pageable,
//                1
//        );
//
//        when(accountRepository.searchAccounts(null, "VENDOR", pageable))
//                .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<AccountDTO> result = accountService.searchAccounts(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals("VEN123", result.getContent().get(0).getAccountCode());
//        assertEquals("VENDOR", result.getContent().get(0).getPartnerType());
//
//        verify(accountRepository).searchAccounts(null, "VENDOR", pageable);
//    }
//
//    @Test
//    void searchAccounts_NoResults() {
//        // Arrange
//        AccountSearchDTO searchDTO = new AccountSearchDTO();
//        searchDTO.setSearchTerm("NONEXISTENT");
//        searchDTO.setPartnerType("CUSTOMER");
//        Pageable pageable = PageRequest.of(0, 10);
//
//        Page<Account> emptyResults = new PageImpl<>(List.of(), pageable, 0);
//
//        when(accountRepository.searchAccounts("NONEXISTENT", "CUSTOMER", pageable))
//                .thenReturn(emptyResults);
//
//        // Act
//        PageResponseDTO<AccountDTO> result = accountService.searchAccounts(searchDTO, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(0, result.getPageDetails().getTotalRecords());
//        assertEquals(0, result.getPageDetails().getTotalPages());
//        assertTrue(result.getContent().isEmpty());
//
//        verify(accountRepository).searchAccounts("NONEXISTENT", "CUSTOMER", pageable);
//    }
//
//    @Test
//    void searchAccounts_NullSearchDTO() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // Use account from setUp
//        Page<Account> searchResults = new PageImpl<>(
//                List.of(account),
//                pageable,
//                1
//        );
//
//        when(accountRepository.searchAccounts(null, null, pageable))
//                .thenReturn(searchResults);
//
//        // Act
//        PageResponseDTO<AccountDTO> result = accountService.searchAccounts(null, pageable);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals(1, result.getPageDetails().getTotalRecords());
//        assertEquals(1, result.getContent().size());
//        assertEquals("ACC123", result.getContent().get(0).getAccountCode());
//
//        verify(accountRepository).searchAccounts(null, null, pageable);
//    }
//}
