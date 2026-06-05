package com.monocept.project.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.monocept.project.enums.ClaimStatus;

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
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long claimId;
	
	@Column(nullable = false, unique = true)
	private String claimNumber;
	
	@ManyToOne
	@JoinColumn(name = "policy_id", nullable = false)
	private Policy policy;
	
	@OneToMany(mappedBy = "claim",
			   cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private List<ClaimDocument> claimDocuments;
	
	@OneToMany(mappedBy = "claim",
			   cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private List<ClaimStatusHistory> claimStatusHistories;
	
	@Column(nullable = false)
    private BigDecimal claimAmount;
	
	@Column(nullable = false)
    private String claimReason;
	
	@Column(nullable = false)
    private LocalDate incidentDate;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus claimStatus = ClaimStatus.SUBMITTED;
	
	private String agentRemarks;

    private String adminRemarks;
    
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
