package com.monocept.project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "customer_id")
	private Long id;
	
	@Column(name = "email_verified")
	private boolean emailVerified = false;

	@Column(name = "phone_verified")
	private boolean phoneVerified = false;
	
	@OneToOne
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;
	
	@Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 10)
    private String pinCode;

    @Column(nullable = false, length = 100)
    private String nomineeName;

    @Column(nullable = false, length = 50)
    private String nomineeRelation;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
    
    @OneToMany(mappedBy = "customer", 
    		   cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Policy> policies;
    
}
