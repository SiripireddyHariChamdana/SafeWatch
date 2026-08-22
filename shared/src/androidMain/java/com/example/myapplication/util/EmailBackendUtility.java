package com.example.myapplication.util;

import okhttp3.*;
import java.io.IOException;

/**
 * Backend Email Utility (Java)
 * Handles SMTP/Email dispatch for OTPs.
 */
public class EmailBackendUtility {
    private static final String API_URL = "https://api.resend.com/emails";
    private static final String API_KEY = "re_S1FqT9u8_CHXv2wXfVf9jY8X9vR8f";
    private static final OkHttpClient client = new OkHttpClient();

    public static void sendOtpEmail(String to, String otp) {
        String json = "{\"from\":\"SafeWatch <onboarding@resend.dev>\", \"to\":\"" + to + "\", \"subject\":\"Safety Verification\", \"html\":\"Your code is: " + otp + "\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException { response.close(); }
        });
    }
}
