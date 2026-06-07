package com.monocept.project.service;

import com.monocept.project.dto.UserRequestDTO;
import com.monocept.project.dto.UserResponseDTO;
import com.monocept.project.dto.UserStatusUpdateRequestDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.Role;

public interface UserService {
    UserResponseDTO createAgent(UserRequestDTO userRequestDTO);
    UserResponseDTO getUserById(Long userId);
    UserResponseDTO getUserByEmail(String email);
    PaginatedResponseDTO<UserResponseDTO> getAllUsers(int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<UserResponseDTO> getUsersByRole(Role role, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<UserResponseDTO> getUsersByStatus(Boolean activeStatus, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<UserResponseDTO> getUsersByRoleAndStatus(Role role, Boolean activeStatus, int page, int size, String sortBy, String direction);
    PaginatedResponseDTO<UserResponseDTO> searchUsersByName(String name, int page, int size, String sortBy, String direction);
    UserResponseDTO updateUserProfile(Long userId, UserRequestDTO userRequestDTO);
    UserResponseDTO updateUserStatus(Long userId, UserStatusUpdateRequestDTO statusUpdateDTO);
}
