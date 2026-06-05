package com.monocept.project.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.monocept.project.enums.PremiumType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "policy_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyPlan {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planId;
	
	@ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct insuranceProduct;
	
	@Column(nullable = false)
    private String planName;
	
	@Column(nullable = false)
    private BigDecimal coverageAmount;
	
	@Column(nullable = false)
    private BigDecimal premiumAmount;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PremiumType premiumType;
	
	@Column(nullable = false)
	private Integer duration;
	
	@Column(nullable = false)
    private String termsAndConditions;
	
	@Column(nullable = false)
    private Boolean activeStatus = true;
	
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
    
    @OneToMany(mappedBy = "policyPlan", 
    		   cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Policy> policies;
    
}