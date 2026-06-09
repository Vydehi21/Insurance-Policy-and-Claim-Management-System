package com.monocept.project.controller;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.UserRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.dto.UserStatusUpdateRequestDTO;
import com.monocept.project.enums.Role;
import com.monocept.project.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "users", description = "Operations for onboarding, editing, auditing, and modifying global system user accounts")
public class UserController {

    private final UserService userService;

    // Admin creates agent
    @PostMapping("/agents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Agent Account", description = "Allows an administrator to safely provision and onboard a new active insurance field agent profile")
    public ResponseEntity<UserResponseDTO> createAgent(
            @Valid @RequestBody UserRequestDTO userRequestDTO) {

        UserResponseDTO response =
                userService.createAgent(userRequestDTO);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    @Operation(summary = "Get User By ID", description = "Retrieves base descriptive account credentials and attributes matching a primary identifier mapping")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userService.getUserById(userId)
        );
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
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
    @Operation(summary = "Get Users By Role Typology", description = "Filters structural profiles cleanly by standard access groups such as Admin, Agent, or Customer")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    @Operation(summary = "Update User Profile", description = "Modifies generic core text parameters inside an established profile record layout body")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequestDTO userRequestDTO) {

        return ResponseEntity.ok(
                userService.updateUserProfile(
                        userId,
                        userRequestDTO)
        );
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
}
