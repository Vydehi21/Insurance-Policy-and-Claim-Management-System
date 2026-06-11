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

    public void sendPasswordResetEmail(
            String email,
            String resetUrl)
            throws IOException, InterruptedException {

        String jsonBody = """
            {
              "from": "onboarding@resend.dev",
              "to": ["%s"],
              "subject": "Reset Your Password",
              "html": "<p>Click below to reset your password:</p><a href='%s'>Reset Password</a>"
            }
            """.formatted(email, resetUrl);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(
                                "https://api.resend.com/emails"))
                        .header(
                                "Authorization",
                                "Bearer " + apiKey)
                        .header(
                                "Content-Type",
                                "application/json")
                        .POST(HttpRequest.BodyPublishers
                                .ofString(jsonBody))
                        .build();

        HttpClient.newHttpClient()
                .send(
                        request,
                        HttpResponse.BodyHandlers.ofString());
    }
}