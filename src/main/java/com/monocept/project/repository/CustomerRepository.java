
package com.monocept.project.repository;

import com.monocept.project.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // FIXED: Changed findByUserId to findByUser_Id
    Optional<Customer> findByUser_Id(Long userId);
	
    // FIXED: Changed existsByUserId to existsByUser_Id
    boolean existsByUser_Id(Long userId);

    Page<Customer> findByUserActiveStatus(
            Boolean activeStatus,
            Pageable pageable);

    Page<Customer> findByUserFullNameContainingIgnoreCase(
            String fullName,
            Pageable pageable);
    
    Optional<Customer> findByUserEmail(String email);

    Optional<Customer> findByUserMobileNumber(String mobileNumber);
}
