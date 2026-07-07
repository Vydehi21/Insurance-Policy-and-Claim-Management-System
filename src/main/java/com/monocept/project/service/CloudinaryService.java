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

    public String uploadFile(MultipartFile file) {
        log.info("Initiating file upload to Cloudinary for file name: {}", file.getOriginalFilename());

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.emptyMap());

            String secureUrl = uploadResult.get("secure_url").toString();
            log.info("File successfully uploaded to Cloudinary. Secure URL generated.");
            return secureUrl;

        } catch (Exception e) {
            // FIX: Replaced e.printStackTrace() with robust logger tracking
            log.error("Cloudinary file upload processing path failed for file: {}", file.getOriginalFilename(), e);

            //  FIX: Throws dedicated custom exception wrapping root cause context securely
            throw new FileUploadException("External cloud storage file upload failed: " + e.getMessage(), e);
        }
    }
}
