package com.example.myapplication.data;

import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Backend Data Manager (Java)
 * Handles networking and Supabase synchronization using OkHttp.
 */
public class SupabaseBackendManager {
    private static final String SUPABASE_URL = "https://mdszyqabsljbrjfcimss.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1kc3p5cWFic2xqYnJqZmNpbXNzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYwMDMwMzMsImV4cCI6MjEwMTU3OTAzM30.g_xrYF9H5xo3wYXBFKPwdnNdn4lNPCNkjeWbwZuKNBM";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .build();

    public interface ContactsCallback {
        void onContactsFetched(List<String> phoneNumbers);
    }

    public static void syncLocation(String userId, double lat, double lng) {
        if (userId == null || userId.isEmpty()) return;
        
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("latitude", lat);
            json.put("longitude", lng);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/user_locations")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) throws IOException { response.close(); }
            });
        } catch (Exception e) {}
    }

    public static void logHistory(String userId, double lat, double lng) {
        if (userId == null || userId.isEmpty()) return;
        System.out.println("🚀 JavaBackend: Logging history point...");
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("latitude", lat);
            json.put("longitude", lng);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/location_history")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    System.out.println("❌ JavaBackend: History log FAILED - " + e.getMessage());
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        System.out.println("✅ JavaBackend: History point saved");
                    } else {
                        System.out.println("❌ JavaBackend: History log ERROR - " + response.code());
                    }
                    response.close();
                }
            });
        } catch (Exception e) {}
    }

    public static void fetchEmergencyContacts(String userId, ContactsCallback callback) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/emergency_contacts?user_id=eq." + userId)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {}
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        List<String> phones = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            phones.add(array.getJSONObject(i).getString("contact_phone"));
                        }
                        callback.onContactsFetched(phones);
                    } catch (Exception e) {}
                }
                response.close();
            }
        });
    }

    public static void uploadEvidence(String userId, String fileName, byte[] data) {
        // userId and timestamp based path as requested
        String timestamp = String.valueOf(System.currentTimeMillis());
        String storagePath = userId + "/" + timestamp + ".m4a";
        
        System.out.println("AUTH_USER_ID: " + userId);
        System.out.println("UPLOAD_STARTED: " + storagePath);
        System.out.println("FILE_SIZE: " + data.length + " bytes");

        if (data.length == 0) {
            System.out.println("UPLOAD_FAILED: File size is 0 bytes");
            return;
        }

        RequestBody requestBody = RequestBody.create(data, MediaType.parse("audio/mp4"));
        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/storage/v1/object/voice-recordings/" + storagePath)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "audio/mp4")
                .addHeader("x-upsert", "true")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                System.out.println("UPLOAD_FAILED: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("UPLOAD_SUCCESS: " + storagePath);
                    // Store metadata in vault after successful storage upload
                    registerInVault(userId, fileName, storagePath);
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    System.out.println("UPLOAD_FAILED: HTTP " + response.code() + " - " + errorBody);
                    // Fallback: If it's a 404, maybe the bucket or user folder doesn't exist? 
                    // But usually post works.
                }
                response.close();
            }
        });
    }

    public static String getPlayableUrl(String storagePath) {
        // Fix: Use the correct path for public object access
        String url = SUPABASE_URL + "/storage/v1/object/public/voice-recordings/" + storagePath + "?apikey=" + SUPABASE_KEY;
        System.out.println("PLAYBACK_URL_CREATED: " + url);
        return url;
    }

    private static void registerInVault(String userId, String fileName, String storagePath) {
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("file_name", fileName);
            json.put("storage_path", storagePath);

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/evidence_vault")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    System.out.println("❌ JavaBackend: Vault registration FAILED - " + e.getMessage());
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        System.out.println("✅ JavaBackend: Metadata saved to evidence_vault");
                    } else {
                        System.out.println("❌ JavaBackend: Vault registration ERROR - " + response.code());
                    }
                    response.close();
                }
            });
        } catch (Exception e) {}
    }

    public static void createSOSAlert(String userId, double lat, double lng, String type) {
        if (userId == null || userId.isEmpty()) return;
        
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("latitude", lat);
            json.put("longitude", lng);
            json.put("trigger_type", type);
            json.put("status", "ACTIVE");

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/sos_alerts")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {}
                @Override public void onResponse(Call call, Response response) throws IOException { response.close(); }
            });
        } catch (Exception e) {}
    }
}
