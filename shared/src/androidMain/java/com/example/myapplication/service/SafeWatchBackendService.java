package com.example.myapplication.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.data.SupabaseBackendManager;
import com.example.myapplication.util.LocationFlow;
import com.example.myapplication.util.ShakeDetector;
import com.example.myapplication.util.SmsRelaySync;
import com.example.myapplication.Platform_androidKt;
import com.google.android.gms.location.*;

public class SafeWatchBackendService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private SensorManager sensorManager;
    private ShakeDetector shakeDetector;
    
    private String currentUserId = null;
    private double lastHistoryLat = 0;
    private double lastHistoryLng = 0;
    private int screenToggleCount = 0;
    private long lastScreenToggleTime = 0;

    private final BroadcastReceiver smsRelayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String phone = intent.getStringExtra("phone");
            String message = intent.getStringExtra("message");
            if (phone != null && message != null) {
                Platform_androidKt.getPlatform().sendNativeSms(phone, message);
            }
        }
    };

    private final BroadcastReceiver hardwareTriggerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_SCREEN_OFF.equals(action)) {
                long now = System.currentTimeMillis();
                if (now - lastScreenToggleTime > 5000) screenToggleCount = 0;
                lastScreenToggleTime = now;
                screenToggleCount++;
                if (screenToggleCount >= 5) {
                    screenToggleCount = 0;
                    vibrateFeedback();
                    triggerEmergencyFlow("HARDWARE_BUTTON");
                }
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level != -1 && scale != -1 && (level * 100 / (float)scale) <= 5.0f) {
                    triggerEmergencyFlow("LOW_BATTERY");
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("SafeWatchService", "🚀 Service onCreate started");
        // Ensure androidContext is initialized for Platform calls in background
        com.example.myapplication.Platform_androidKt.androidContext = this.getApplicationContext();

        currentUserId = getSharedPreferences("SafeWatch", MODE_PRIVATE).getString("uid", null);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(() -> {
            vibrateFeedback();
            triggerEmergencyFlow("SHAKE");
            return kotlin.Unit.INSTANCE;
        });
        
        // Fix: Use SENSOR_DELAY_UI to avoid SecurityException crash on Android 12+
        sensorManager.registerListener(shakeDetector, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        
        IntentFilter hFilter = new IntentFilter();
        hFilter.addAction(Intent.ACTION_SCREEN_ON);
        hFilter.addAction(Intent.ACTION_SCREEN_OFF);
        hFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(hardwareTriggerReceiver, hFilter);

        IntentFilter rFilter = new IntentFilter("com.safewatch.SMS_RELAY");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsRelayReceiver, rFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(smsRelayReceiver, rFilter);
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result != null && result.getLastLocation() != null) {
                    processLocationUpdate(result.getLastLocation());
                }
            }
        };
    }

    private void processLocationUpdate(Location loc) {
        double lat = loc.getLatitude();
        double lng = loc.getLongitude();
        LocationFlow.INSTANCE.updateLocation(lat, lng, loc.getAccuracy());

        if (currentUserId != null) {
            SupabaseBackendManager.syncLocation(currentUserId, lat, lng);
            float[] res = new float[1];
            Location.distanceBetween(lastHistoryLat, lastHistoryLng, lat, lng, res);
            if (lastHistoryLat == 0 || res[0] > 2.0) {
                lastHistoryLat = lat; lastHistoryLng = lng;
                SupabaseBackendManager.logHistory(currentUserId, lat, lng);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void triggerEmergencyFlow(String type) {
        if (currentUserId == null) currentUserId = getSharedPreferences("SafeWatch", MODE_PRIVATE).getString("uid", null);
        if (currentUserId == null) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) SupabaseBackendManager.createSOSAlert(currentUserId, location.getLatitude(), location.getLongitude(), type);
        });

        Platform_androidKt.getPlatform().startManualRecording();
        SupabaseBackendManager.fetchEmergencyContacts(currentUserId, phoneNumbers -> {
            String link = "https://majestic-pudding-3979e7.netlify.app/?id=" + currentUserId;
            String msg = "🚨 EMERGENCY SOS! Track me: " + link;
            for (String p : phoneNumbers) if (p != null) Platform_androidKt.getPlatform().sendNativeSms(p, msg);
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String path = Platform_androidKt.getPlatform().stopManualRecording();
            if (path != null && currentUserId != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    byte[] b = Platform_androidKt.getPlatform().readFileBytes(path);
                    if (b != null && b.length > 0) SupabaseBackendManager.uploadEvidence(currentUserId, "SOS_" + System.currentTimeMillis() + ".m4a", b);
                }, 1000);
            }
        }, 15000);
    }

    private void vibrateFeedback() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(500);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        
        // Always try to retrieve the UID from storage if not in intent
        if (currentUserId == null) {
            currentUserId = getSharedPreferences("SafeWatch", MODE_PRIVATE).getString("uid", null);
        }

        if (intent != null) {
            if (intent.hasExtra("USER_ID")) {
                currentUserId = intent.getStringExtra("USER_ID");
                getSharedPreferences("SafeWatch", MODE_PRIVATE).edit().putString("uid", currentUserId).apply();
            }
            if ("TRIGGER_SOS".equals(intent.getAction())) triggerEmergencyFlow("MANUAL");
        }
        
        if (currentUserId != null && !currentUserId.isEmpty()) {
            Log.i("SafeWatchService", "📡 Restarting SMS Relay Sync for: " + currentUserId);
            SmsRelaySync.INSTANCE.startSync(currentUserId);
        } else {
            Log.w("SafeWatchService", "⚠️ Cannot start SMS Relay: User ID is NULL");
        }

        fusedLocationClient.requestLocationUpdates(new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build(), locationCallback, null);
        return START_STICKY;
    }

    private Notification createNotification() {
        String id = "safewatch_backend";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(id) == null) nm.createNotificationChannel(new NotificationChannel(id, "Safety Active", NotificationManager.IMPORTANCE_LOW));
        }
        return new NotificationCompat.Builder(this, id).setContentTitle("SafeWatch Backend Active").setSmallIcon(android.R.drawable.ic_menu_mylocation).build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(hardwareTriggerReceiver); } catch (Exception e) {}
        try { unregisterReceiver(smsRelayReceiver); } catch (Exception e) {}
        sensorManager.unregisterListener(shakeDetector);
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
