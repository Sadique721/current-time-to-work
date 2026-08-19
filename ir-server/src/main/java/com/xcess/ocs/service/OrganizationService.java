package com.xcess.ocs.service;

import com.xcess.ocs.dto.OrganizationDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.OrganizationSearchDTO;
import com.xcess.ocs.entity.Organization;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.OrganizationMapper;
import com.xcess.ocs.repository.OrganizationRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all organization operations.
 * 
 * What it does:
 * - Create new organization (checks for duplicate name and legalName)
 * - Update existing organization (checks if id exists and for duplicate values)
 * - Delete organization (prevents deletion if partners exist)
 * - Search organizations (supports pagination and search by name/legalName/address)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    /**
     * Get all active organizations.
     */
    public List<OrganizationDTO> getAllOrganizations() {
        log.debug("Fetching all active organizations");
        return organizationRepository.findAll().stream()
                .map(organizationMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get organization by ID.
     * 
     * @param id - organization id
     * @return organization details
     * @throws ResourceNotFoundException if id not found
     */
    public OrganizationDTO getOrganizationById(Long id) {
        log.debug("Fetching organization with ID: {}", id);
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        return organizationMapper.toDTO(organization);
    }

    /**
     * Create new organization.
     * 
     * Steps:
     * 1. Check if name already exists
     * 2. Check if legalName already exists
     * 3. Save new organization
     * 
     * @param dto - organization data from user
     * @return created organization
     * @throws DuplicateNameException if name or legalName already exists
     */
    @Transactional
    public OrganizationDTO createOrganization(OrganizationDTO dto) {
        log.info("Creating new organization: {}", dto.getName());
        
        // Check duplicate name
        if (organizationRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            log.warn("Organization name already exists: {}", dto.getName());
            throw new DuplicateNameException("Organization", dto.getName());
        }

        // Check duplicate legalName
        if (organizationRepository.existsByLegalNameAndIsDeletedFalse(dto.getLegalName())) {
            log.warn("Organization legal name already exists: {}", dto.getLegalName());
            throw new DuplicateNameException("Organization", dto.getLegalName());
        }

        // Save to database
        log.info("Creating new organization with name: {}", dto.getName());
        Organization organization = organizationMapper.toEntity(dto);
        Organization saved = organizationRepository.save(organization);
        log.info("Successfully created organization with ID: {}", saved.getOrganizationId());
        return organizationMapper.toDTO(saved);
    }

    /**
     * Update existing organization.
     * 
     * Steps:
     * 1. Check if id exists (throw error if not)
     * 2. If name changed, check if new name already exists
     * 3. If legalName changed, check if new legalName already exists
     * 4. Update and save organization
     * 
     * @param id - organization id to update
     * @param dto - new organization data
     * @return updated organization
     * @throws ResourceNotFoundException if id not found
     * @throws DuplicateNameException if name or legalName already exists
     */
    @Transactional
    public OrganizationDTO updateOrganization(Long id, OrganizationDTO dto) {
        log.info("Updating organization with ID: {}", id);
        
        // Check if organization exists
        Organization existing = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        // Check if name is changed and duplicate
        if (!existing.getName().equals(dto.getName())) {
            if (organizationRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
                log.warn("Organization name already exists: {}", dto.getName());
                throw new DuplicateNameException("Organization", dto.getName());
            }
            existing.setName(dto.getName());
        }

        // Check if legalName is changed and duplicate
        if (!existing.getLegalName().equals(dto.getLegalName())) {
            if (organizationRepository.existsByLegalNameAndIsDeletedFalse(dto.getLegalName())) {
                log.warn("Organization legal name already exists: {}", dto.getLegalName());
                throw new DuplicateNameException("Organization", dto.getLegalName());
            }
            existing.setLegalName(dto.getLegalName());
        }

        // Update remaining fields
        existing.setSuffixName(dto.getSuffixName());
        existing.setLegalName(dto.getLegalName());
        existing.setAddress(dto.getAddress());

        // Save to database
        Organization updated = organizationRepository.save(existing);
        log.info("Successfully updated organization with ID: {}", updated.getOrganizationId());
        return organizationMapper.toDTO(updated);
    }

    /**
     * Delete organization (soft delete).
     * 
     * Steps:
     * 1. Check if id exists (throw error if not)
     * 2. Check if organization has active partners
     * 3. Delete organization
     * 
     * @param id - organization id to delete
     * @throws ResourceNotFoundException if id not found
     * @throws ForeignReferenceException if organization has active partners
     */
    @Transactional
    public void deleteOrganization(Long id) {
        log.info("Deleting organization with ID: {}", id);
        
        // Check if organization exists
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        // Check if partners exist
        if (organizationRepository.existsActivePartnersByOrganizationId(id)) {
            log.warn("Cannot delete organization with active partners: {}", id);
            throw new ForeignReferenceException("Organization cannot be deleted because it has active partners");
        }

        // Delete from database
        organizationRepository.delete(organization);
        log.info("Successfully deleted organization with ID: {}", id);
    }

    /**
     * Search organizations by name, legalName, address or suffixName.
     * 
     * @param searchDTO - contains search term (optional)
     * @param pageable - page number and size
     * @return list of organizations
     */
    public PageResponseDTO<OrganizationDTO> searchOrganizations(OrganizationSearchDTO searchDTO, Pageable pageable) {
        log.debug("Searching organizations with criteria: {}", searchDTO.getSearchTerm());
        
        String searchTerm = searchDTO != null ? searchDTO.getSearchTerm() : null;
        Page<Organization> organizationsPage = organizationRepository.searchOrganizations(searchTerm, pageable);
        
        List<OrganizationDTO> organizations = organizationsPage.getContent().stream()
                .map(organizationMapper::toDTO)
                .collect(Collectors.toList());
        
        log.debug("Found {} organizations matching criteria", organizations.size());
        return PaginationUtils.buildGetResponseDTO(organizations, organizationsPage);
    }

    /**
     * Get organizations in pages.
     * 
     * @param pageable - page number and size
     * @return list of organizations
     */
    public PageResponseDTO<OrganizationDTO> getOrganizationsInPage(Pageable pageable) {
        log.debug("Fetching organizations in page no: {}", pageable.getPageNumber());
        Page<Organization> organizationPage = organizationRepository.findAll(pageable);
        List<OrganizationDTO> organizations = organizationPage.getContent().stream()
                .map(organizationMapper::toDTO)
                .collect(Collectors.toList());
        log.debug("Retrieved {} organizations in page", organizations.size());
        return PaginationUtils.buildGetResponseDTO(organizations, organizationPage);
    }

    /**
     * Get organization id and name list for dropdown.
     * 
     * @return list of organization id and name
     */
    public List<java.util.Map<String, Object>> getOrganizationIdAndNameList() {
        log.debug("Fetching organization IDs and names for dropdown");
        return organizationRepository.findAll().stream()
                .map(org -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("organizationId", org.getOrganizationId());
                    map.put("name", org.getName());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
