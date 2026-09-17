package com.monocept.project.model;

import java.time.LocalDateTime;
import java.util.List;

import com.monocept.project.enums.Role;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_status", columnList = "activeStatus"),
        @Index(name = "idx_assigned_product", columnList = "assigned_product_id"),
        @Index(name = "idx_reset_token", columnList = "resetToken")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;
	
	@Column(nullable = false)
	private String fullName;
	
	@Column(nullable = false, unique = true)
	private String email;
	
	@Column(nullable = false)
    private String password;

    @Column(nullable = false,unique = true)
    private String mobileNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @Column(nullable = false)
    private Boolean activeStatus = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @Column(nullable = false)
    private LocalDateTime updatedDate;
    
    private String resetToken;

    private LocalDateTime resetTokenExpiry;

    // Set whenever the password changes. JWTs issued before this moment are
    // rejected, so a password reset signs out every other session.
    private LocalDateTime passwordChangedAt;
    
    @PrePersist
    public void beforeSave() {
    	createdDate = LocalDateTime.now();
    	updatedDate = LocalDateTime.now();
    }
    
    @PreUpdate
    public void beforeUpdate() {
    	updatedDate = LocalDateTime.now();
    }
    
    @OneToOne(mappedBy = "user",
    		  cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Customer customer;
    
    @OneToMany(mappedBy = "user")
    private List<ClaimStatusHistory> claimStatusHistories;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_product_id")
    private InsuranceProduct assignedProduct;
}