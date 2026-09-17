package com.monocept.project.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.monocept.project.exception.InvalidRequestException;
import com.monocept.project.security.CustomUserDetails;
import com.monocept.project.service.CloudinaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final CloudinaryService cloudinaryService;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "application/pdf");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB

    @PostMapping("/upload")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (file.isEmpty()) {
            throw new InvalidRequestException("File cannot be empty");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new InvalidRequestException("Only JPEG, PNG, and PDF files are allowed");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new InvalidRequestException("File size must not exceed 5MB");
        }

        // The Content-Type header is chosen by the client, so a renamed .html or
        // .exe passed the check above. Confirm the file content really is the
        // declared type by reading its first bytes.
        if (!contentMatchesDeclaredType(file)) {
            throw new InvalidRequestException(
                    "The file content doesn't match its type. Only real JPEG, PNG, and PDF files are allowed");
        }

        String fileUrl = cloudinaryService.uploadFile(file, userDetails.getUserId());

        return ResponseEntity.ok(Map.of("fileUrl", fileUrl));
    }

    private boolean contentMatchesDeclaredType(MultipartFile file) {
        byte[] header = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            throw new InvalidRequestException("Unable to read the uploaded file");
        }

        String type = file.getContentType();

        if ("application/pdf".equals(type)) {
            // "%PDF"
            return read >= 4 && header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46;
        }
        if ("image/png".equals(type)) {
            // 89 50 4E 47 0D 0A 1A 0A
            return read >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E
                    && header[3] == 0x47 && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A
                    && header[7] == 0x0A;
        }
        if ("image/jpeg".equals(type)) {
            // FF D8 FF
            return read >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
        }
        return false;
    }
}