package com.monocept.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    private long planId;
	
	@ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct insuranceProduct;
	
	@Column(nullable = false)
    private String planName;
	
	@Column(nullable = false)
    private double coverageAmount;
	
	@Column(nullable = false)
    private double premiumAmount;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PremiumType premiumType;
}
