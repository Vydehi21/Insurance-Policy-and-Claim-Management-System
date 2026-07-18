package com.monocept.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monocept.project.dto.AssignProductRequestDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.UpdateStaffRequestDTO;
import com.monocept.project.dto.UserRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.dto.UserStatusUpdateRequestDTO;
import com.monocept.project.enums.Role;
import com.monocept.project.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173/")
@Tag(name = "users", description = "Operations for onboarding, editing, auditing, and modifying global system user accounts")
public class UserController {

    private final UserService userService;

    // Admin creates internal staff
    @PostMapping("/internal-staff")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Internal Staff Account", description = "Allows an administrator to safely provision and onboard a new active insurance field internal staff profile")
    public ResponseEntity<UserResponseDTO> createInternalStaff(
            @Valid @RequestBody UserRequestDTO userRequestDTO) {

        UserResponseDTO response =
                userService.createInternalStaff(userRequestDTO);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL_STAFF', 'CUSTOMER')")
    @Operation(summary = "Get User By ID", description = "Retrieves base descriptive account credentials and attributes matching a primary identifier mapping")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userService.getUserById(userId)
        );
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL_STAFF')")
    @Operation(summary = "Get User By Email", description = "Runs a precise database lookup to track down a unique user account profile using an email registration address string")
    public ResponseEntity<UserResponseDTO> getUserByEmail(
            @PathVariable String email) {

        return ResponseEntity.ok(
                userService.getUserByEmail(email)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Registered Users", description = "Generates a paginated list of all operational and dormant credentials across the entire platform ecosystem")
    public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                userService.getAllUsers(
                        page,
                        size,
                        sortBy,
                        direction)
        );
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Users By Role Typology", description = "Filters structural profiles cleanly by standard access groups such as Admin, Internal Staff, or Customer")
    public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> getUsersByRole(
            @PathVariable Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                userService.getUsersByRole(
                        role,
                        page,
                        size,
                        sortBy,
                        direction)
        );
    }
    
    @GetMapping("/status/{activeStatus}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Users By Status", description = "Isolates catalog structures to evaluate records based on live operational active flags vs disabled credentials")
    public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> getUsersByStatus(
            @PathVariable Boolean activeStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                userService.getUsersByStatus(
                        activeStatus,
                        page,
                        size,
                        sortBy,
                        direction)
        );
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Agent Profile", description = "Modifies agent demographic parameters securely using a dedicated payload model wrapper")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UpdateStaffRequestDTO updateStaffRequestDTO) {

        // 🛠️ Routes directly to your cleaner, password-free profile update method
        return ResponseEntity.ok(userService.updateInternalStaffProfile(userId, updateStaffRequestDTO));
    }


    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update User Status", description = "Directly flags credentials to perform structural locking or unlocking sequences across consumer profiles")
    public ResponseEntity<UserResponseDTO> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequestDTO requestDTO) {

        return ResponseEntity.ok(
                userService.updateUserStatus(
                        userId,
                        requestDTO)
        );
    }

    // §4.4 — new capability outside the original SRS: admin (re)assigns which
    // insurance product an internal-staff user is scoped to, or clears it by
    // sending a null productId.
    @PutMapping("/{userId}/assign-product")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign Product To Internal Staff", description = "Allows an administrator to scope an internal staff account to a single insurance product, or clear an existing assignment")
    public ResponseEntity<UserResponseDTO> assignProductToUser(
            @PathVariable Long userId,
            @RequestBody AssignProductRequestDTO requestDTO) {

        return ResponseEntity.ok(
                userService.assignProductToUser(
                        userId,
                        requestDTO.getProductId())
        );
    }
}