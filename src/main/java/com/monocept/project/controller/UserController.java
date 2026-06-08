package com.monocept.project.controller;

import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.dto.UserRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.dto.UserStatusUpdateRequestDTO;
import com.monocept.project.enums.Role;
import com.monocept.project.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Admin creates agent
    @PostMapping("/agents")
    public ResponseEntity<UserResponseDTO> createAgent(
            @Valid @RequestBody UserRequestDTO userRequestDTO) {

        UserResponseDTO response =
                userService.createAgent(userRequestDTO);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userService.getUserById(userId)
        );
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponseDTO> getUserByEmail(
            @PathVariable String email) {

        return ResponseEntity.ok(
                userService.getUserByEmail(email)
        );
    }

    @GetMapping
    public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> getAllUsers(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdDate")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction) {


        return ResponseEntity.ok(
                userService.getAllUsers(
                        page,
                        size,
                        sortBy,
                        direction)
        );
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> getUsersByRole(

            @PathVariable Role role,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdDate")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction) {


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
    public ResponseEntity<PaginatedResponseDTO<UserResponseDTO>> getUsersByStatus(

            @PathVariable Boolean activeStatus,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdDate")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String direction) {


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