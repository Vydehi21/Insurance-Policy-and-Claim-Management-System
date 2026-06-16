package com.monocept.project.repository;

import com.monocept.project.model.User;
import com.monocept.project.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByRole(Role role, Pageable pageable);
    Page<User> findByActiveStatus(Boolean activeStatus, Pageable pageable);
    Page<User> findByRoleAndActiveStatus(Role role, Boolean activeStatus, Pageable pageable);
    Page<User> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    Optional<User> findByMobileNumber(String mobileNumber);

}
