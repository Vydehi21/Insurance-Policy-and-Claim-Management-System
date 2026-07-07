package com.monocept.project.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.exception.InvalidRequestException;

public class PaginationUtil {

    private PaginationUtil() {
    }

    public static Pageable createPageable(int page, int size, String sortBy, String direction) {
        validatePagination(page, size);

        return PageRequest.of(page, size, Sort.by(getSortDirection(direction), sortBy));
    }

    public static <T> PaginatedResponseDTO<T> createPaginatedResponse(Page<T> page, String sortBy, String direction) {

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

    private static void validatePagination(int page, int size) {
        if(page < 0)
            throw new InvalidRequestException("Page number cannot be negative");

        if(size <= 0)
            throw new InvalidRequestException("Page size must be greater than zero");

        if(size > 100)
            throw new InvalidRequestException("Page size cannot exceed 100");
    }

    private static Sort.Direction getSortDirection(String direction) {
        if(direction == null || direction.equalsIgnoreCase("asc"))
            return Sort.Direction.ASC;

        if(direction.equalsIgnoreCase("desc"))
            return Sort.Direction.DESC;

        throw new InvalidRequestException("Sort direction must be asc or desc");
    }
    
    public static Pageable buildPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) throw new InvalidRequestException("Page number cannot be negative");
        if (size <= 0 || size > 100) throw new InvalidRequestException("Page size must be between 1 and 100");
        String dir = (direction == null) ? "asc" : direction;
        Sort sort = dir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
}