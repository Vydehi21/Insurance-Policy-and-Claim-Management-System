package com.monocept.project.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.monocept.project.enums.ClaimStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Index;

@Entity
@Table(
    name = "claims",
    indexes = {
        @Index(name = "idx_claim_number", columnList = "claimNumber"),
        @Index(name = "idx_claim_status", columnList = "claimStatus"),
        @Index(name = "idx_policy_id", columnList = "policy_id"),
        @Index(name = "idx_created_date", columnList = "createdDate")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "claim_id")
	private Long id;
	
	@Column(nullable = false, unique = true)
	private String claimNumber;
	
	@ManyToOne
	@JoinColumn(name = "policy_id", nullable = false)
	private Policy policy;
	
	@OneToMany(
		    mappedBy = "claim",
		    cascade = CascadeType.ALL,
		    orphanRemoval = true
		)
	private List<ClaimDocument> claimDocuments;
	
	@OneToMany(
		    mappedBy = "claim",
		    cascade = CascadeType.ALL,
		    orphanRemoval = true
		)
	private List<ClaimStatusHistory> claimStatusHistories;
	
	@Column(nullable = false)
    private BigDecimal claimAmount;
	
	@Column(nullable = false)
    private String claimReason;
	
	@Column(nullable = false)
    private LocalDate incidentDate;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private ClaimStatus claimStatus = ClaimStatus.SUBMITTED;
	
	private String internalStaffRemarks;

    private String adminRemarks;
    
    @ManyToOne
    @JoinColumn(name="reviewed_by")
    private User reviewedBy;


    // When the current reviewer took the review lock. A lock older than the
    // configured timeout (or held by a deactivated user) can be taken over.
    private LocalDateTime reviewLockedAt;

    @ManyToOne
    @JoinColumn(name="decided_by")
    private User decidedBy;
    
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