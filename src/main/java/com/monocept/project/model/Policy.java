package com.monocept.project.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.monocept.project.enums.PolicyStatus;
import com.monocept.project.enums.PremiumType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Policy {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "policy_id")
	private Long id;
	
	@Column(nullable = false, unique = true)
	private String policyNumber;
	
	@ManyToOne
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;
	
	@ManyToOne
	@JoinColumn(name = "plan_id", nullable = false)
	private PolicyPlan policyPlan;

	// NEW: the customer's own choices at purchase time, and the premium
	// computed from them. This — not policyPlan.coverageAmount /
	// policyPlan.premiumAmount — is authoritative for this specific policy.
	@Column(nullable = false)
	private BigDecimal coverageAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PremiumType premiumType;

	@Column(nullable = false)
	private BigDecimal premiumAmount;
	
	@OneToMany(mappedBy = "policy",
			   cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private List<PremiumPayment> premiumPayments;
	
	@OneToMany(mappedBy = "policy",
			   cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private List<Claim> claims;
	
	@Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus policyStatus = PolicyStatus.PENDING_PAYMENT;
    
    @Column(nullable = false)
    private BigDecimal totalPremiumPaid = BigDecimal.ZERO;
    
    @Column(name = "next_premium_due_date")
    private LocalDate nextPremiumDueDate;
    
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