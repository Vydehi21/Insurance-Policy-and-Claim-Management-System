package com.monocept.project.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.monocept.project.enums.PolicyStatus;

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
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Policy {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long policyId;
	
	@Column(nullable = false, unique = true)
	private String policyNumber;
	
	@ManyToOne
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;
	
	@ManyToOne
	@JoinColumn(name = "plan_id", nullable = false)
	private PolicyPlan policyPlan;
	
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
