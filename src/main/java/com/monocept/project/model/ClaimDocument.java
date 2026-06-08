package com.monocept.project.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "claim_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDocument {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "claim_id", nullable = false)
	private Claim claim;
	
	@Column(nullable = false)
    private String documentName;

    @Column(nullable = false)
    private String documentType;

    @Column(nullable = false)
    private String documentReference;

    @Column(nullable = false)
    private LocalDateTime uploadedDate;

    @PrePersist
    public void beforeSave() {
        uploadedDate = LocalDateTime.now();
    }
}
