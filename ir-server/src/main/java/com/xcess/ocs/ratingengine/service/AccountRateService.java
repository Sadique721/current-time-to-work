package com.xcess.ocs.ratingengine.service;

import com.xcess.ocs.entity.*;
import com.xcess.ocs.exception.RateLookupException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.repository.AccountRepository;
import com.xcess.ocs.roaming.entity.CallType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountRateService {
    private final AccountRepository accountRepository;

    @Cacheable(value = "accountRatePackage", key = "{#accountId, #callTime, #serviceType}")
    @Transactional(readOnly = true)
    public RatePackage findRatePackageForAccount(String accountId, LocalDateTime callTime, ServiceType serviceType) {
        if (accountId == null) {
            log.warn("Null accountId provided to findRatePackageForAccount");
            return null;
        }

        try {
            Optional<Account> accountOpt = accountRepository.findByAccountCodeAndIsDeletedFalse(accountId);

            if (accountOpt.isEmpty()) {
                log.warn("Account not found with code: {}", accountId);
                return null;
            }

            Account account = accountOpt.get();
            log.debug("Found account: {}, account type: {}", account.getAccountCode(), account.getAccountType());

            ProductPlan productPlan = account.getProductPlan();
            if (productPlan == null) {
                log.warn("No product plan associated with account: {}", accountId);
                return null;
            }
            log.debug("Found product plan: {}", productPlan.getName());

            RatePackage ratePackage = findRatePackageByServiceType(productPlan, account.getAccountType(), serviceType, callTime, null);

            if (ratePackage != null) {
                log.debug("Found rate package {} for account {}", ratePackage.getPackageName(), accountId);
            } else {
                log.warn("No rate package found for account {} with type {}", accountId, account.getAccountType());
            }

            return ratePackage;
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found during rate lookup for account {}: {}", accountId, e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for rate lookup for account {}: {}", accountId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error finding rate package for account {}: {}", accountId, e.getMessage(), e);
            throw new RateLookupException("Rate lookup failed for account: " + accountId, e);
        }
    }

    private RatePackage findRatePackageByServiceType(ProductPlan productPlan, String accountType,
                                                     ServiceType serviceType, LocalDateTime callTime, CallType callType) {
        List<ProductPlanAssociation> associations = productPlan.getRatePackageGroups();
        if (associations == null || associations.isEmpty()) {
            log.warn("No rate package group associations found for product plan: {}", productPlan.getName());
            return null;
        }

        for (ProductPlanAssociation assoc : associations) {
            if (serviceType != null && !serviceType.equals(assoc.getRatePackageGroup().getServiceType())) {
                continue;
            }

            RatePackageGroup group = assoc.getRatePackageGroup();
            RatePackage ratePackage = tryPackagesInGroup(group, accountType, callTime, callType);
            if (ratePackage != null) {
                return ratePackage;
            }
        }

        log.debug("No rate package found after checking all associations");
        return null;
    }

    private RatePackage tryPackagesInGroup(RatePackageGroup group, String accountType,
                                           LocalDateTime callTime, CallType callType) {
        List<RatePackageAssociation> packageAssociations = group.getRatePackageAssociations();
        if (packageAssociations == null || packageAssociations.isEmpty()) {
            return null;
        }

        if (callType == null) {
            packageAssociations.sort(Comparator.comparing(RatePackageAssociation::getPriority,
                    Comparator.nullsLast(Comparator.naturalOrder())));

            for (RatePackageAssociation assoc : packageAssociations) {
                if (!isTimeInRange(callTime, assoc.getStartTime(), assoc.getEndTime())) {
                    continue;
                }

                RatePackage pkg = assoc.getRatePackage();
                if (pkg == null) {
                    continue;
                }

                if (!isPackageApplicableForType(pkg, accountType)) {
                    continue;
                }

                log.debug("Found applicable package: {} from group: {}", pkg.getPackageName(), group.getName());
                return pkg;
            }
        } else {
            for (RatePackageAssociation assoc : packageAssociations) {
                if (!isTimeInRange(callTime, assoc.getStartTime(), assoc.getEndTime())) {
                    continue;
                }

                if (callType != null && !callType.equals(assoc.getCallType())) {
                    continue;
                }

                RatePackage pkg = assoc.getRatePackage();
                if (pkg == null) {
                    continue;
                }

                if (!isPackageApplicableForType(pkg, accountType)) {
                    continue;
                }

                log.debug("Found applicable package: {} from group: {}", pkg.getPackageName(), group.getName());
                return pkg;
            }
        }

        return null;
    }

    private boolean isTimeInRange(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return true;
        }
        return (time.isEqual(start) || time.isAfter(start)) &&
               (time.isBefore(end) || time.isEqual(end));
    }

    private boolean isPackageApplicableForType(RatePackage ratePackage, String accountType) {
        if (accountType == null || ratePackage == null || ratePackage.getType() == null) {
            return false;
        }

        switch (ratePackage.getType()) {
            case SELLING:
                return "CUSTOMER".equals(accountType) || "BOTH".equals(accountType);
            case BUYING:
                return "VENDOR".equals(accountType) || "BOTH".equals(accountType);
            default:
                return false;
        }
    }
}
