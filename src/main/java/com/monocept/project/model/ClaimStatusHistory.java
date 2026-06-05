package com.monocept.project.model;

import java.time.LocalDateTime;

import com.monocept.project.enums.ClaimStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "claim_status_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long historyId;
	
	@ManyToOne
	@JoinColumn(name = "claim_id", nullable = false)
	private Claim claim;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus previousStatus;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus newStatus;
	
	@Column(nullable = false)
    private String remarks;
	
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	
	@Column(nullable = false, updatable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    public void beforeSave() {
        updatedDate = LocalDateTime.now();
    }
}
