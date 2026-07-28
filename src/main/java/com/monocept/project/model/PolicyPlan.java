package com.monocept.project.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.monocept.project.enums.PremiumType;

import jakarta.persistence.*;
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
	@Column(name = "plan_id")
    private Long id;
	
	@ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct insuranceProduct;
	
	@Column(nullable = false)
    private String planName;

	// Minimum coverage a customer may choose at purchase time.
	@Column(nullable = false)
	private BigDecimal minCoverageAmount;

	// Maximum coverage a customer may choose at purchase time.
	@Column(nullable = false)
	private BigDecimal maxCoverageAmount;

	// Premium charged per ₹50,000 of coverage, per year. This is the
	// admin-controlled pricing knob — the actual premium a customer pays is
	// always computed server-side from this rate + their chosen coverage,
	// never entered directly.
	@Column(nullable = false)
	private BigDecimal ratePerUnit;

	// Discount applied when a customer pays ANNUAL (0-100).
	@Column(nullable = false)
	private BigDecimal annualDiscountPercent = BigDecimal.ZERO;

	// Discount applied when a customer pays ONE_TIME (0-100).
	@Column(nullable = false)
	private BigDecimal oneTimeDiscountPercent = BigDecimal.ZERO;

	// Default/display payment frequency for this plan. No longer
	// authoritative for pricing — Policy.premiumType (chosen by the
	// customer) is what actually determines how their premium is computed.
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