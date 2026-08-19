package com.xcess.ocs.mapper;

import com.xcess.ocs.entity.Account;
import com.xcess.ocs.dto.AccountDTO;
import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.entity.ProductPlan;

public class AccountMapper {

    public static AccountDTO mapToDTO(Account account) {
        if (account == null) return null;
        AccountDTO dto = new AccountDTO();
        dto.setAccountId(account.getAccountId());
        dto.setAccountCode(account.getAccountCode());
        dto.setAccountType(account.getAccountType());

        if (account.getPartner() != null) {
            dto.setPartnerId(account.getPartner().getPartnerId());
            dto.setPartnerName(account.getPartner().getPartnerName());
        }
        if (account.getProductPlan() != null) {
            dto.setProductPlanId(account.getProductPlan().getProductPlanId());
            dto.setProductPlanName(account.getProductPlan().getName());
        }
        return dto;
    }

    public static Account mapToEntity(AccountDTO dto, Partner partner, ProductPlan productPlan) {
        if (dto == null) return null;
        Account account = new Account();
        account.setAccountId(dto.getAccountId());
        account.setAccountCode(dto.getAccountCode());
        account.setAccountType(dto.getAccountType());
        account.setPartner(partner);
        account.setProductPlan(productPlan);
        return account;
    }
}