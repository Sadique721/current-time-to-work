package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.PartnerDTO;
import com.xcess.ocs.entity.ClearingHouse;
import com.xcess.ocs.entity.Organization;
import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.repository.ClearingHouseRepository;
import com.xcess.ocs.repository.OrganizationRepository;
import com.xcess.ocs.roaming.repository.TapProfileGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartnerMapper {

    private final OrganizationRepository organizationRepository;
    private final ClearingHouseRepository clearingHouseRepository;
    private final TapProfileGroupRepository tapProfileGroupRepository;

    public PartnerDTO toDto(Partner partner) {
        if (partner == null) return null;

        PartnerDTO dto = new PartnerDTO();
        dto.setPartnerId(partner.getPartnerId());
        dto.setPartnerName(partner.getPartnerName());
        dto.setPartnerCode(partner.getPartnerCode());
        dto.setPartnerType(partner.getPartnerType());
        dto.setStatus(partner.getStatus());
        dto.setCountry(partner.getCountry());
        dto.setContactPersonName(partner.getContactPersonName());
        dto.setEmail(partner.getEmail());
        dto.setPhoneNumber(partner.getPhoneNumber());
        dto.setAddressLine1(partner.getAddressLine1());
        dto.setCity(partner.getCity());
        dto.setPostalCode(partner.getPostalCode());
        dto.setInterconnectType(partner.getInterconnectType());
        dto.setPointCode(partner.getPointCode());
        dto.setIpAddress(partner.getIpAddress());
        dto.setRoutingPrefix(partner.getRoutingPrefix());
        dto.setBillingCurrency(partner.getBillingCurrency());
        dto.setBillingCycle(partner.getBillingCycle());
        dto.setPaymentTerms(partner.getPaymentTerms());
        dto.setTaxNumber(partner.getTaxNumber());
        dto.setBankAccountNumber(partner.getBankAccountNumber());
        dto.setSwiftCode(partner.getSwiftCode());
        dto.setTadigCode(partner.getTadigCode());
        dto.setHplmn(partner.getHplmn());
        dto.setLineOfBusiness(partner.getLineOfBusiness());
        dto.setTapVersion(partner.getTapVersion());
        dto.setTapSftpRouteType(partner.getSftpRouteType());
        dto.setSftpHost(partner.getSftpHost());
        dto.setSftpPort(partner.getSftpPort());
        dto.setSftpUsername(partner.getSftpUsername());
        dto.setSftpPassword(partner.getSftpPassword());
        dto.setSftpRemotePath(partner.getSftpRemotePath());
        dto.setSftpInboxPath(partner.getSftpInboxPath());

        if (partner.getOrganization() != null) {
            dto.setOrganizationId(partner.getOrganization().getOrganizationId());
        }
        if (partner.getClearingHouse() != null) {
            dto.setClearingHouseId(partner.getClearingHouse().getId());
        }
        if (partner.getTapProfileGroup() != null) {
            dto.setTapProfileGroupId(partner.getTapProfileGroup().getId());
            dto.setTapProfileGroupName(partner.getTapProfileGroup().getName());
        }

        return dto;
    }

    public Partner toEntity(PartnerDTO dto, Organization organization) {
        if (dto == null) return null;

        Partner partner = new Partner();
        partner.setPartnerId(dto.getPartnerId());
        partner.setPartnerName(dto.getPartnerName());
        partner.setPartnerCode(dto.getPartnerCode());
        partner.setPartnerType(dto.getPartnerType());
        partner.setStatus(dto.getStatus());
        partner.setCountry(dto.getCountry());
        partner.setContactPersonName(dto.getContactPersonName());
        partner.setEmail(dto.getEmail());
        partner.setPhoneNumber(dto.getPhoneNumber());
        partner.setAddressLine1(dto.getAddressLine1());
        partner.setCity(dto.getCity());
        partner.setPostalCode(dto.getPostalCode());
        partner.setInterconnectType(dto.getInterconnectType());
        partner.setPointCode(dto.getPointCode());
        partner.setIpAddress(dto.getIpAddress());
        partner.setRoutingPrefix(dto.getRoutingPrefix());
        partner.setBillingCurrency(dto.getBillingCurrency());
        partner.setBillingCycle(dto.getBillingCycle());
        partner.setPaymentTerms(dto.getPaymentTerms());
        partner.setTaxNumber(dto.getTaxNumber());
        partner.setBankAccountNumber(dto.getBankAccountNumber());
        partner.setSwiftCode(dto.getSwiftCode());
        partner.setTadigCode(dto.getTadigCode());
        partner.setHplmn(dto.getHplmn());
        partner.setLineOfBusiness(dto.getLineOfBusiness());
        partner.setTapVersion(dto.getTapVersion());
        partner.setSftpRouteType(dto.getTapSftpRouteType());
        partner.setSftpHost(dto.getSftpHost());
        partner.setSftpPort(dto.getSftpPort());
        partner.setSftpUsername(dto.getSftpUsername());
        partner.setSftpPassword(dto.getSftpPassword());
        partner.setSftpRemotePath(dto.getSftpRemotePath());
        partner.setSftpInboxPath(dto.getSftpInboxPath());

        if (dto.getOrganizationId() != null) {
            partner.setOrganization(organizationRepository.findById(dto.getOrganizationId()).orElse(null));
        }
        if (dto.getClearingHouseId() != null) {
            ClearingHouse clearingHouse = clearingHouseRepository.findByIdAndIsDeletedFalse(dto.getClearingHouseId()).orElse(null);
            partner.setClearingHouse(clearingHouse);
        }
        if (dto.getTapProfileGroupId() != null) {
            partner.setTapProfileGroup(tapProfileGroupRepository.findById(dto.getTapProfileGroupId()).orElse(null));
        }

        return partner;
    }
}
