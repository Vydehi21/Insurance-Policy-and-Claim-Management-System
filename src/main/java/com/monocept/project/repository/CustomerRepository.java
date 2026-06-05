package com.monocept.project.repository;

import com.monocept.project.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByLinkedUser_UserId(Long userId);
    Page<Customer> findByLinkedUser_FullNameContainingIgnoreCase(String fullName, Pageable pageable);
}
