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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private String fullName;
	
	@Column(nullable = false, unique = true)
	private String email;
	
	@Column(nullable = false)
    private String password;

    @Column(nullable = false)
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
    
    @Column(length = 10)
    private String resetPasswordOtp;

    private LocalDateTime resetPasswordOtpExpiry;
    
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
}
