package com.monocept.project.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.monocept.project.exception.InvalidRequestException;
import com.monocept.project.service.CloudinaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final CloudinaryService cloudinaryService;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "application/pdf");
    private static final long MAX_SIZE = 5 * 1024 * 1024; 
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file) {


        if(file.isEmpty()){

            return ResponseEntity
                    .badRequest()
                    .body("File cannot be empty");

        }
        
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidRequestException("Only JPEG, PNG, and PDF files are allowed");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new InvalidRequestException("File size must not exceed 5MB");
        }


        String fileUrl =
                cloudinaryService.uploadFile(file);


        return ResponseEntity.ok(
                Map.of(
                    "fileUrl",
                    fileUrl
                )
        );

    }
}