package com.xcess.ocs.service;

import com.xcess.ocs.dto.AccountDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.AccountSearchDTO;
import com.xcess.ocs.entity.Account;
import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.entity.ProductPlan;
import com.xcess.ocs.exception.DuplicateAccountCodeException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.AccountMapper;
import com.xcess.ocs.repository.AccountRepository;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.repository.ProductPlanRepository;
import com.xcess.ocs.util.PaginationUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private ProductPlanRepository productPlanRepository;

    @Transactional
    public AccountDTO createAccount(AccountDTO accountDTO) {
        log.info("Creating new account with code: {}", accountDTO.getAccountCode());
        try {
            Partner partner = partnerRepository.findById(accountDTO.getPartnerId())
                    .orElseThrow(() -> {
                        log.error("Partner not found with ID: {}", accountDTO.getPartnerId());
                        return new ResourceNotFoundException("Partner not found with ID: " + accountDTO.getPartnerId());
                    });
            if(partner.getLineOfBusiness().equals(LineOfBusiness.INTERCONNECT))
            {
                if (accountRepository.existsByAccountCodeAndIsDeletedFalse(accountDTO.getAccountCode())) {
                    log.warn("Attempt to create account with existing code: {}", accountDTO.getAccountCode());
                    throw new DuplicateAccountCodeException(accountDTO.getAccountCode());
                }
            }

            ProductPlan productPlan = productPlanRepository.findById(accountDTO.getProductPlanId())
                    .orElseThrow(() -> {
                        log.error("Product Plan not found with ID: {}", accountDTO.getProductPlanId());
                        return new ResourceNotFoundException(
                                "Product Plan not found with ID: " + accountDTO.getProductPlanId());
                    });
            // LOB-specific duplicate checks
            if (partner.getLineOfBusiness().equals(LineOfBusiness.ROAMING)) {
                // ROAMING: Unique Home PLMN + Product Plan combination
                if (accountRepository.existsByAccountCodeAndProductPlanIdAndIsDeletedFalse(
                        accountDTO.getAccountCode(), accountDTO.getProductPlanId(), null)) {
                    log.warn("Duplicate Home PLMN {} for product plan {}", accountDTO.getAccountCode(), accountDTO.getProductPlanId());
                    throw new DuplicateAccountCodeException(partner.getPartnerName(),
                            "Account with same Home PLMN and product plan already exists for Roaming Partner");
                }
            }

            Account account = AccountMapper.mapToEntity(accountDTO, partner, productPlan);
            Account savedAccount = accountRepository.save(account);
            log.info("Successfully created account with ID: {}", savedAccount.getAccountId());
            return AccountMapper.mapToDTO(savedAccount);
        } catch (Exception e) {
            log.error("Error creating account: {}", e.getMessage(), e);
            throw e;
        }
    }

    public PageResponseDTO<AccountDTO> getAccountsInPages(Pageable pageable) {
        log.debug("Fetching accounts in pages");
        Page<Account> accountPage = accountRepository.findAll(pageable);
        List<AccountDTO> accountDTOs = accountPage.getContent().stream()
                .map(AccountMapper::mapToDTO)
                .collect(Collectors.toList());
        log.info("Retrieved {} accounts in a page", accountDTOs.size());
        return PaginationUtils.buildGetResponseDTO(accountDTOs, accountPage);
    }

    public List<AccountDTO> getAllAccounts() {
        log.debug("Fetching all accounts");
        List<AccountDTO> accounts = accountRepository.findAll().stream()
                .map(AccountMapper::mapToDTO)
                .collect(Collectors.toList());
        log.debug("Retrieved {} accounts", accounts.size());
        return accounts;
    }

    public AccountDTO getAccountById(Long id) {
        log.debug("Fetching account with ID: {}", id);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Account not found with ID: {}", id);
                    return new ResourceNotFoundException("Account not found with ID: " + id);
                });
        return AccountMapper.mapToDTO(account);
    }

    public PageResponseDTO<AccountDTO> searchAccounts(AccountSearchDTO accountSearchDTO, Pageable pageable) {
        log.debug("Searching accounts with criteria: {}", accountSearchDTO);
        String searchTerm = accountSearchDTO != null ? accountSearchDTO.getSearchTerm() : null;
        String accountType = accountSearchDTO != null ? accountSearchDTO.getAccountType() : null;
        Page<Account> accountsPage = accountRepository.searchAccounts(
                searchTerm,
                accountType,
                pageable
        );
        List<AccountDTO> accounts = accountsPage.getContent().stream()
                .map(AccountMapper::mapToDTO)
                .collect(Collectors.toList());
        log.debug("Found {} accounts matching criteria", accounts.size());
        return PaginationUtils.buildGetResponseDTO(accounts, accountsPage);
    }

    public List<AccountDTO> getAccountsByPartnerId(Long partnerId) {
        log.debug("Fetching accounts for partner ID: {}", partnerId);
        List<Account> accounts = accountRepository.findByPartnerPartnerId(partnerId);
        if (accounts.isEmpty()) {
            log.warn("No accounts found for partner ID: {}", partnerId);
            throw new ResourceNotFoundException("No accounts found for partner ID: " + partnerId);
        }
        List<AccountDTO> dtos = accounts.stream().map(AccountMapper::mapToDTO).collect(Collectors.toList());
        log.debug("Retrieved {} accounts for partner ID: {}", dtos.size(), partnerId);
        return dtos;
    }

    @Transactional
    public AccountDTO updateAccount(Long id, AccountDTO accountDTO) {
        log.info("Updating account with ID: {}", id);
        try {
            Account existingAccount = accountRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Account not found with ID: {}", id);
                        return new ResourceNotFoundException("Account not found with ID: " + id);
                    });
            Partner partner = partnerRepository.findById(accountDTO.getPartnerId())
                    .orElseThrow(() -> {
                        log.error("Partner not found with ID: {}", accountDTO.getPartnerId());
                        return new ResourceNotFoundException("Partner not found with ID: " + accountDTO.getPartnerId());
                    });
            ProductPlan productPlan = productPlanRepository.findById(accountDTO.getProductPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product Plan not found with ID: " + accountDTO.getProductPlanId()));
            // LOB-specific duplicate checks for update
            if (partner.getLineOfBusiness().equals(LineOfBusiness.INTERCONNECT)) {
                // INTERCONNECT: Check if account code is changed and new code already exists
                if (!existingAccount.getAccountCode().equalsIgnoreCase(accountDTO.getAccountCode()) &&
                        accountRepository.existsByAccountCodeAndIsDeletedFalse(accountDTO.getAccountCode())) {
                    log.warn("Attempt to update account to existing code: {}", accountDTO.getAccountCode());
                    throw new DuplicateAccountCodeException(accountDTO.getAccountCode());
                }
            } else if (partner.getLineOfBusiness().equals(LineOfBusiness.ROAMING)) {
                // ROAMING: Check if Home PLMN or Product Plan is changed, exclude current account
                boolean isHomePlmnChanged = !existingAccount.getAccountCode().equalsIgnoreCase(accountDTO.getAccountCode());
                boolean isProductPlanChanged = !existingAccount.getProductPlan().getProductPlanId().equals(accountDTO.getProductPlanId());

                if (isHomePlmnChanged || isProductPlanChanged) {
                    if (accountRepository.existsByAccountCodeAndProductPlanIdAndIsDeletedFalse(
                            accountDTO.getAccountCode(), accountDTO.getProductPlanId(), existingAccount.getAccountId())) {
                        log.warn("Duplicate Home PLMN {} for product plan {} when updating account {}",
                                accountDTO.getAccountCode(), accountDTO.getProductPlanId(), id);
                        throw new DuplicateAccountCodeException(partner.getPartnerName(),
                                "Account with same Home PLMN and product plan already exists for Roaming Partner");
                    }
                }
            }
            existingAccount.setAccountCode(accountDTO.getAccountCode());
            existingAccount.setAccountType(accountDTO.getAccountType());
            existingAccount.setPartner(partner);
            existingAccount.setProductPlan(productPlan);
            Account updatedAccount = accountRepository.save(existingAccount);
            log.info("Successfully updated account with ID: {}", id);
            return AccountMapper.mapToDTO(updatedAccount);
        } catch (Exception e) {
            log.error("Error updating account with ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void deleteAccount(Long id) {
        log.info("Deleting account with ID: {}", id);
        try {
            if (!accountRepository.existsById(id)) {
                log.warn("Attempt to delete non-existent account with ID: {}", id);
                throw new ResourceNotFoundException("Account not found with ID: " + id);
            }
            accountRepository.deleteById(id);
            log.info("Successfully deleted account with ID: {}", id);
        } catch (Exception e) {
            log.error("Error deleting account with ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    private void validateAccountType(String accountType, String partnerType) {
        log.debug("Validating account type: {} against partner type: {}", accountType, partnerType);
        if (!com.xcess.ocs.entity.PartnerType.CUSTOMER.name().equals(accountType)
                && !com.xcess.ocs.entity.PartnerType.VENDOR.name().equals(accountType)) {
            log.warn("Invalid account type: {}", accountType);
            throw new IllegalArgumentException("Account type must be either CUSTOMER or VENDOR");
        }

        if (!com.xcess.ocs.entity.PartnerType.BOTH.name().equals(partnerType) && !partnerType.equals(accountType)) {
            log.warn("Incompatible account type: {} with partner type: {}", accountType, partnerType);
            throw new IllegalArgumentException(
                    "Account type '" + accountType + "' is not compatible with partner type '" + partnerType + "'");
        }
    }

    public List<Map<String,Object>> getAccountCodesByPartnerId(Long partnerId) {

        List<Account> accounts=accountRepository.findByPartnerPartnerId(partnerId);
        return accounts.stream()
                .map(a -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("accountCode",a.getAccountCode( ));
                    map.put("accountType", a.getAccountType());
                    map.put("accountId",a.getAccountId());
                    return map;
                })
                .collect(Collectors.toList());
    }
}