package com.xcess.ocs.service;

import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.AgreementSearchDTO;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgreementService {

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private AccountAgreementRepository accountAgreementRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TemplateConfigurationRepository templateConfigurationRepository;

    @Autowired
    private TaxConfigRepository taxConfigRepository;

    @Autowired
    private AgreementTaxConfigRepository agreementTaxConfigRepository;

    @Autowired
    private StateRepository stateRepository;

    @Transactional
    public AgreementDTO createAgreement(AgreementDTO dto) {
        dto.validate();
        validateTaxFields(dto);

        if (agreementRepository.existsByAgreementCode(dto.getAgreementCode())) {
            throw new RuntimeException("Agreement code already exists");
        }

        Agreement agreement = new Agreement();
        agreement.setAgreementCode(dto.getAgreementCode());
        agreement.setBillingCycleStartDate(dto.getBillingCycleStartDate());
        agreement.setNextBillingCycleStartDate(dto.getBillingCycleStartDate());
        agreement.setBillingCyclePeriod(dto.getBillingCyclePeriod());

        agreement.setIsIncomingSettlement(dto.getIsIncomingSettlement());
        agreement.setIsOutgoingSettlement(dto.getIsOutgoingSettlement());
        agreement.setIsNetSettlement(dto.getIsNetSettlement());

        agreement.setLineOfBusiness(dto.getLineOfBusiness());

        if (dto.getLineOfBusiness().equals(LineOfBusiness.ROAMING)) {
            if (dto.getTapDirection() == null) {
                throw new IllegalArgumentException("TapDirection is required for Roaming");
            }
        }

        agreement.setTapDirection(dto.getTapDirection());

        if (dto.getIsIncomingSettlement() && dto.getIncomingSettlementTemplateId() != null && dto.getIncomingSettlementTemplateId() > 0) {
            TemplateConfiguration template = templateConfigurationRepository.findById(dto.getIncomingSettlementTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Incoming template not found"));
            agreement.setIncomingSettlementTemplate(template);
        }
        if (dto.getIsOutgoingSettlement() && dto.getOutgoingSettlementTemplateId() != null && dto.getOutgoingSettlementTemplateId() > 0) {
            TemplateConfiguration template = templateConfigurationRepository.findById(dto.getOutgoingSettlementTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Outgoing template not found"));
            agreement.setOutgoingSettlementTemplate(template);
        }
        if (dto.getIsNetSettlement() && dto.getNetSettlementTemplateId() != null && dto.getNetSettlementTemplateId() > 0) {
            TemplateConfiguration template = templateConfigurationRepository.findById(dto.getNetSettlementTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Net template not found"));
            agreement.setNetSettlementTemplate(template);
        }

        setTaxExempt(dto, agreement);
        agreement = agreementRepository.save(agreement);
        setAgreementTaxConfigs(dto, agreement);

        if (dto.getAccountAgreements() != null) {
            for (AccountAgreementDTO aaDto : dto.getAccountAgreements()) {
                Account account = accountRepository.findById(aaDto.getAccountId())
                        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

                AccountAgreement aa = new AccountAgreement();
                aa.setAgreement(agreement);
                aa.setAccount(account);
                aa.setInvoiceFormat(aaDto.getInvoiceFormat());
                accountAgreementRepository.save(aa);
            }
        }

        return toDTO(agreement);
    }

    public AgreementDTO getAgreementById(Long id) {
        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found"));
        AgreementDTO agreementDTO = toDTO(agreement);
        return agreementDTO;
    }

    public List<AgreementDTO> getAllAgreements() {
        return agreementRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AgreementDTO updateAgreement(Long id, AgreementDTO dto) {
        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement not found"));

        dto.validate();
        validateTaxFields(dto);

        if (!agreement.getAgreementCode().equals(dto.getAgreementCode())) {
            throw new RuntimeException("Agreement code cannot be modified");
        }

        agreement.setBillingCycleStartDate(dto.getBillingCycleStartDate());
        agreement.setNextBillingCycleStartDate(dto.getBillingCycleStartDate());
        agreement.setBillingCyclePeriod(dto.getBillingCyclePeriod());

        agreement.setIsIncomingSettlement(dto.getIsIncomingSettlement());
        agreement.setIsOutgoingSettlement(dto.getIsOutgoingSettlement());
        agreement.setIsNetSettlement(dto.getIsNetSettlement());

        if (dto.getLineOfBusiness().equals(LineOfBusiness.ROAMING)) {
            if (dto.getTapDirection() == null) {
                throw new IllegalArgumentException("TapDirection is required for Roaming");
            }
        }

        agreement.setTapDirection(dto.getTapDirection());

        if (dto.getIsIncomingSettlement() && dto.getIncomingSettlementTemplateId() != null && dto.getIncomingSettlementTemplateId() > 0) {
            TemplateConfiguration template = templateConfigurationRepository.findById(dto.getIncomingSettlementTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Incoming template not found"));
            agreement.setIncomingSettlementTemplate(template);
        }
        if (dto.getIsOutgoingSettlement() && dto.getOutgoingSettlementTemplateId() != null && dto.getOutgoingSettlementTemplateId() > 0) {
            TemplateConfiguration template = templateConfigurationRepository.findById(dto.getOutgoingSettlementTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Outgoing template not found"));
            agreement.setOutgoingSettlementTemplate(template);
        }
        if (dto.getIsNetSettlement() && dto.getNetSettlementTemplateId() != null && dto.getNetSettlementTemplateId() > 0) {
            TemplateConfiguration template = templateConfigurationRepository.findById(dto.getNetSettlementTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Net template not found"));
            agreement.setNetSettlementTemplate(template);
        }

        setTaxExempt(dto, agreement);
        updateAgreementTaxConfigs(dto, agreement);
        agreement = agreementRepository.save(agreement);

        return toDTO(agreement);
    }

    @Transactional
    public void deleteAgreement(Long id) {
        if (!agreementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Agreement not found");
        }
        agreementTaxConfigRepository.deleteByAgreement_AgreementId(id);
        accountAgreementRepository.deleteByAgreement_AgreementId(id);
        agreementRepository.deleteById(id);
    }

    private void setTaxExempt(AgreementDTO dto, Agreement agreement) {
        if (dto.getIsTaxExempt() != null) {
            agreement.setIsTaxExempt(dto.getIsTaxExempt());
        }
    }


    private void validateTaxFields(AgreementDTO dto) {
        boolean taxExempt = Boolean.TRUE.equals(dto.getIsTaxExempt());

        if (!taxExempt) {
            if (dto.getTaxConfigs() == null || dto.getTaxConfigs().isEmpty()) {
                throw new IllegalArgumentException("At least one tax config is required");
            }

            Set<Long> seenTaxConfigIds = new HashSet<>();
            for (int i = 0; i < dto.getTaxConfigs().size(); i++) {
                AgreementTaxConfigDTO atcDto = dto.getTaxConfigs().get(i);
                TaxConfig tc = taxConfigRepository.findById(atcDto.getTaxConfigId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Tax config not found: " + atcDto.getTaxConfigId()));

                if (!seenTaxConfigIds.add(atcDto.getTaxConfigId())) {
                    throw new IllegalArgumentException(
                            "Duplicate tax config: " + tc.getTaxName() + " selected in multiple rows");
                }

                if (i == 0) {
                    if (!"BASE".equals(tc.getApplyOn())) {
                        throw new IllegalArgumentException(
                                "First tax config must have apply_on = BASE, got: " + tc.getApplyOn());
                    }
                    if (atcDto.getAccumulateFromOrders() != null
                            && !atcDto.getAccumulateFromOrders().isBlank()) {
                        throw new IllegalArgumentException(
                                "First tax config must be BASE (accumulate_from_orders must be null)");
                    }
                } else {
                    if (!"CUMULATIVE".equals(tc.getApplyOn())) {
                        throw new IllegalArgumentException(
                                "Tax config at position " + (i + 1)
                                        + " must have apply_on = CUMULATIVE, got: " + tc.getApplyOn());
                    }
                    if (atcDto.getAccumulateFromOrders() == null
                            || atcDto.getAccumulateFromOrders().isBlank()) {
                        throw new IllegalArgumentException(
                                "Tax config at position " + (i + 1)
                                        + " must have accumulate_from_orders set (CUMULATIVE tax)");
                    }
                }

                if (atcDto.getAccumulateFromOrders() != null
                        && !atcDto.getAccumulateFromOrders().isBlank()) {
                    for (String ref : atcDto.getAccumulateFromOrders().split(",")) {
                        int refOrder = Integer.parseInt(ref.trim());

                        boolean refExists = dto.getTaxConfigs().stream()
                                .anyMatch(a -> a.getApplyOrder().equals(refOrder));
                        if (!refExists) {
                            throw new IllegalArgumentException(
                                    "Order " + refOrder + " referenced but not found");
                        }

                        if (refOrder >= atcDto.getApplyOrder()) {
                            throw new IllegalArgumentException(
                                    "Order " + atcDto.getApplyOrder()
                                            + " cannot accumulate from order " + refOrder
                                            + " (must be lower order)");
                        }
                    }
                }
            }
        }
    }

    private void setAgreementTaxConfigs(AgreementDTO dto, Agreement agreement) {
        if (dto.getTaxConfigs() != null && !dto.getTaxConfigs().isEmpty()) {
            List<AgreementTaxConfig> atcList = new ArrayList<>();
            for (AgreementTaxConfigDTO atcDto : dto.getTaxConfigs()) {
                TaxConfig taxConfig = taxConfigRepository.findById(atcDto.getTaxConfigId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tax config not found: " + atcDto.getTaxConfigId()));

                AgreementTaxConfig atc = new AgreementTaxConfig();
                atc.setAgreement(agreement);
                atc.setTaxConfig(taxConfig);
                atc.setApplyOrder(atcDto.getApplyOrder());
                atc.setAccumulateFromOrders(atcDto.getAccumulateFromOrders());
                atcList.add(atc);
            }
            agreement.setAgreementTaxConfigs(atcList);
        }
    }

    private void updateAgreementTaxConfigs(AgreementDTO dto, Agreement agreement) {
        agreement.getAgreementTaxConfigs().size();
        List<AgreementTaxConfigDTO> newTaxConfigs = dto.getTaxConfigs();
        if (newTaxConfigs != null && !newTaxConfigs.isEmpty()) {
            Map<Integer, AgreementTaxConfig> currentByOrder = agreement.getAgreementTaxConfigs().stream()
                    .collect(Collectors.toMap(AgreementTaxConfig::getApplyOrder, atc -> atc, (a, b) -> a));
            Set<Integer> desiredOrders = newTaxConfigs.stream()
                    .map(AgreementTaxConfigDTO::getApplyOrder)
                    .collect(Collectors.toSet());

            agreement.getAgreementTaxConfigs().removeIf(atc -> !desiredOrders.contains(atc.getApplyOrder()));

            for (AgreementTaxConfigDTO atcDto : newTaxConfigs) {
                TaxConfig taxConfig = taxConfigRepository.findById(atcDto.getTaxConfigId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tax config not found: " + atcDto.getTaxConfigId()));
                AgreementTaxConfig existing = currentByOrder.get(atcDto.getApplyOrder());
                if (existing != null) {
                    existing.setTaxConfig(taxConfig);
                    existing.setAccumulateFromOrders(atcDto.getAccumulateFromOrders());
                } else {
                    AgreementTaxConfig atc = new AgreementTaxConfig();
                    atc.setAgreement(agreement);
                    atc.setTaxConfig(taxConfig);
                    atc.setApplyOrder(atcDto.getApplyOrder());
                    atc.setAccumulateFromOrders(atcDto.getAccumulateFromOrders());
                    agreement.getAgreementTaxConfigs().add(atc);
                }
            }
        } else {
            agreement.getAgreementTaxConfigs().clear();
        }
    }

    private AgreementDTO toDTO(Agreement agreement) {
        AgreementDTO dto = new AgreementDTO();
        dto.setAgreementId(agreement.getAgreementId());
        dto.setAgreementCode(agreement.getAgreementCode());
        dto.setBillingCycleStartDate(agreement.getBillingCycleStartDate());
        dto.setBillingCyclePeriod(agreement.getBillingCyclePeriod());

        dto.setIsIncomingSettlement(agreement.getIsIncomingSettlement());
        dto.setIsOutgoingSettlement(agreement.getIsOutgoingSettlement());
        dto.setIsNetSettlement(agreement.getIsNetSettlement());

        if (agreement.getIncomingSettlementTemplate() != null) {
            dto.setIncomingSettlementTemplateId(agreement.getIncomingSettlementTemplate().getTemplateId());
            dto.setIncomingSettlementTemplateName(agreement.getIncomingSettlementTemplate().getTemplateName());
        }
        if (agreement.getOutgoingSettlementTemplate() != null) {
            dto.setOutgoingSettlementTemplateId(agreement.getOutgoingSettlementTemplate().getTemplateId());
            dto.setOutgoingSettlementTemplateName(agreement.getOutgoingSettlementTemplate().getTemplateName());
        }
        if (agreement.getNetSettlementTemplate() != null) {
            dto.setNetSettlementTemplateId(agreement.getNetSettlementTemplate().getTemplateId());
            dto.setNetSettlementTemplateName(agreement.getNetSettlementTemplate().getTemplateName());
        }

        List<AccountAgreementDTO> aaDtos = accountAgreementRepository
                .findByAgreement_AgreementId(agreement.getAgreementId())
                .stream()
                .map(aa -> {
                    AccountAgreementDTO aaDto = new AccountAgreementDTO();
                    aaDto.setAccountAgreementId(aa.getAccountAgreementId());
                    aaDto.setAccountId(aa.getAccount().getAccountId());
                    aaDto.setAccountCode(aa.getAccount().getAccountCode());
                    aaDto.setAccountType(aa.getAccount().getAccountType());
                    aaDto.setInvoiceFormat(aa.getInvoiceFormat());
                    return aaDto;
                })
                .collect(Collectors.toList());

        dto.setAccountAgreements(aaDtos);

        if (agreement.getTapDirection() != null) {
            dto.setTapDirection(agreement.getTapDirection());
        }
        if (agreement.getLineOfBusiness() != null) {
            dto.setLineOfBusiness(agreement.getLineOfBusiness());
        }

        if (agreement.getIsTaxExempt() != null) {
            dto.setIsTaxExempt(agreement.getIsTaxExempt());
        }

        List<AgreementTaxConfigDTO> taxConfigDtos = agreementTaxConfigRepository
                .findByAgreement_AgreementIdOrderByApplyOrderAsc(agreement.getAgreementId())
                .stream()
                .map(atc -> {
                    AgreementTaxConfigDTO atcDto = new AgreementTaxConfigDTO();
                    atcDto.setId(atc.getId());
                    atcDto.setTaxConfigId(atc.getTaxConfig().getTaxConfigId());
                    atcDto.setTaxConfigName(atc.getTaxConfig().getTaxName() + " (" + atc.getTaxConfig().getTaxType() + ")");
                    atcDto.setTaxType(atc.getTaxConfig().getTaxType());
                    atcDto.setApplyOrder(atc.getApplyOrder());
                    atcDto.setAccumulateFromOrders(atc.getAccumulateFromOrders());
                    return atcDto;
                })
                .collect(Collectors.toList());
        dto.setTaxConfigs(taxConfigDtos);

        if (agreement.getAccountAgreements() != null && !agreement.getAccountAgreements().isEmpty()) {
            dto.setPartnerName(agreement.getAccountAgreements().iterator().next().getAccount().getPartner().getPartnerName());
        } else {
            dto.setPartnerName("null");
        }

        return dto;
    }

    public PageResponseDTO<AgreementDTO> getAgreementsInPage(Pageable pageable) {
        Page<Agreement> page = agreementRepository.findAll(pageable);
        return buildPageResponse(page);
    }

    public PageResponseDTO<AgreementDTO> searchAgreements(AgreementSearchDTO searchDTO, Pageable pageable) {
        Page<Agreement> page = agreementRepository.findByAgreementCodeContainingIgnoreCase(searchDTO.getSearchTerm(), pageable);
        return buildPageResponse(page);
    }

    private PageResponseDTO<AgreementDTO> buildPageResponse(Page<Agreement> page) {
        PaginationDetailsDTO pageDetails = new PaginationDetailsDTO(
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber() + 1
        );
        List<AgreementDTO> content = page.getContent().stream()
                .map(this::toDTO)
                .toList();
        return new PageResponseDTO<>(pageDetails, content);
    }
}
