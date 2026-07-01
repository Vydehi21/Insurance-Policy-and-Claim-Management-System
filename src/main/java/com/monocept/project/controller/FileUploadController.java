package com.monocept.project.controller;

import com.monocept.project.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final CloudinaryService cloudinaryService;
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file) {


        if(file.isEmpty()){

            return ResponseEntity
                    .badRequest()
                    .body("File cannot be empty");

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