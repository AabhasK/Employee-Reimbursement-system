package com.reimbursement.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SignupMailer {

    private static final String ADDRESS = "[a-zA-Z0-9._+%-]+@[a-zA-Z0-9.-]+";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String apiKey;
    private final String from;

    public SignupMailer(String apiKey, String from) {
        this.apiKey = apiKey;
        this.from = from;
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank() && from != null && from.matches(ADDRESS);
    }

    public void send(String email, String code) throws IOException, InterruptedException {
        if (!configured() || !email.matches(ADDRESS) || !code.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("Invalid signup email settings");
        }
        String json = """
                {"from":"%s","to":["%s"],"subject":"Your Thapar signup code",
                "text":"Your verification code is %s. It expires in 10 minutes. If you did not request it, you can ignore this email."}
                """.formatted(from, email, code);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("Mail provider returned HTTP " + status);
        }
    }
}
