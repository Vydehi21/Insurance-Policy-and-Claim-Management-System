package com.monocept.project.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.monocept.project.exception.FileUploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Folder a customer's claim documents are uploaded to. Claim submission
     * only accepts document URLs inside the submitting customer's own folder,
     * so one customer can't attach a file another customer uploaded.
     */
    public static String claimFolderFor(Long userId) {
        return "claims/user-" + userId;
    }

    public String uploadFile(MultipartFile file, Long uploaderUserId) {

        log.info("Uploading claim document to Cloudinary for user id {}", uploaderUserId);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", claimFolderFor(uploaderUserId)));

            String secureUrl = uploadResult.get("secure_url").toString();

            log.info("File successfully uploaded to Cloudinary. Secure URL generated.");

            return secureUrl;

        } catch (Exception e) {
            log.error("Cloudinary file upload failed for user id {}", uploaderUserId, e);
            throw new FileUploadException("External cloud storage file upload failed: " + e.getMessage(), e);
        }
    }
}