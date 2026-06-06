package com.monocept.project.util;

import org.springframework.data.domain.Page;

import com.monocept.project.dto.PaginatedResponseDTO;

public class PaginationUtil {

    private PaginationUtil() {
    }

    public static <T> PaginatedResponseDTO<T> createPaginatedResponse(
            Page<T> page,
            String sortBy,
            String direction) {

        PaginatedResponseDTO<T> response = new PaginatedResponseDTO<>();

        response.setRecords(page.getContent());
        response.setCurrentPage(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalRecords(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setIsLastPage(page.isLast());
        response.setSortField(sortBy);
        response.setSortDirection(direction);

        return response;
    }
}