package com.monocept.project.model;

import java.time.LocalDateTime;

import com.monocept.project.enums.ClaimStatus;

import jakarta.persistence.*;
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
	@Column(name = "history_id")
	private Long id;
	
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
