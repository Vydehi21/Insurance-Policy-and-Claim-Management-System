package com.monocept.project.model;

import java.time.LocalDateTime;
import java.util.List;

import com.monocept.project.enums.ProductType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Index;

@Entity
@Table(
    name = "insurance_products",
    indexes = {
        @Index(name = "idx_product_type", columnList = "productType"),
        @Index(name = "idx_product_status", columnList = "activeStatus")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Long id;
	
	@Column(nullable = false, unique = true)
	private String productName;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProductType productType;
	
	@Column(nullable = false)
	private String description;
	
	@Column(nullable = false)
    private Boolean activeStatus = true;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long version = 0L;

    @OneToMany(mappedBy = "insuranceProduct",
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PolicyPlan> plans;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    public void beforeSave() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedDate = LocalDateTime.now();

    }
	
}