package com.xcess.ocs.util;

import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.PaginationDetailsDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public class PaginationUtils {

    public static <D, T> PageResponseDTO<D> buildGetResponseDTO(List<D> content, Page<T> page){
        PaginationDetailsDTO pageDetailsDTO = new PaginationDetailsDTO();
        pageDetailsDTO.setCurrentPageNumber(page.getNumber() + 1); // Adding 1 to make it 1-based index
        pageDetailsDTO.setTotalRecordsPerPage(page.getSize());
        pageDetailsDTO.setTotalPages(page.getTotalPages());
        pageDetailsDTO.setTotalRecords(page.getTotalElements());

        PageResponseDTO<D> pageResponseDTO = new PageResponseDTO<>();
        pageResponseDTO.setContent(content);
        pageResponseDTO.setPageDetails(pageDetailsDTO);
        return pageResponseDTO;
    }
}
