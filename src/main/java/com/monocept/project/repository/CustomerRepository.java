
package com.monocept.project.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monocept.project.model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // FIXED: Changed findByUserId to findByUser_Id
    Optional<Customer> findByUser_Id(Long userId);
	
    // FIXED: Changed existsByUserId to existsByUser_Id
    boolean existsByUser_Id(Long userId);

    Page<Customer> findByUserActiveStatus(
            Boolean activeStatus,
            Pageable pageable);

    @Query("""
    		SELECT c
    		FROM Customer c
    		WHERE

    		LOWER(c.user.fullName)
    		LIKE LOWER(CONCAT('%', :keyword, '%'))

    		OR

    		LOWER(c.user.email)
    		LIKE LOWER(CONCAT('%', :keyword, '%'))

    		OR

    		LOWER(c.user.mobileNumber)
    		LIKE LOWER(CONCAT('%', :keyword, '%'))

    		OR

    		LOWER(c.city)
    		LIKE LOWER(CONCAT('%', :keyword, '%'))

    		OR

    		STR(c.id)
    		LIKE CONCAT('%', :keyword, '%')
    		""")
    		Page<Customer> searchCustomers(
    		        @Param("keyword") String keyword,
    		        Pageable pageable
    		);
    
    Optional<Customer> findByUserEmail(String email);

    Optional<Customer> findByUserMobileNumber(String mobileNumber);
}
