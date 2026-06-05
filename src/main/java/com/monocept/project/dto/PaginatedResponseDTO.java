package com.monocept.project.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponseDTO<T> {
    private List<T> records;
    private Integer currentPage;
    private Integer pageSize;
    private Long totalRecords;
    private Integer totalPages;
    private Boolean isLastPage;
    private String sortField;
    private String sortDirection;
}
