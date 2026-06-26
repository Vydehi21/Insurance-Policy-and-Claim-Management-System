package com.monocept.project.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    public void sendPasswordResetEmail(String email, String resetUrl) 
            throws IOException, InterruptedException {

        String jsonBody = """
            {
              "from": "onboarding@resend.dev",
              "to": ["%s"],
              "subject": "Reset Your Insurance System Password",
              "html": "<p>You requested a password reset. Please click the button below to choose a new password:</p><a href='%s' style='display:inline-block; background-color:#0d6efd; color:#ffffff; padding:10px 20px; text-decoration:none; border-radius:5px; font-weight:bold; margin:15px 0;'>Reset Password</a><p>If the button above does not work, copy and paste this link into your browser:</p><p>%s</p><p>This secure link will expire in 15 minutes.</p>"
            }
            """.formatted(email, resetUrl, resetUrl);

        // ... Keep HTTP Request and HttpClient logic exactly the same ...


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }
}
