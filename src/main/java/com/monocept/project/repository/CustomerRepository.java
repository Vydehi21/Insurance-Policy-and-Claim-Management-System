package com.monocept.project.repository;

import com.monocept.project.model.Customer;
import com.monocept.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByLinkedUser(User user);
    Optional<Customer> findByLinkedUser_UserId(Long userId);
}
