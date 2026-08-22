package com.example.myapplication

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.telephony.SmsManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.myapplication.service.SafeWatchBackendService
import com.example.myapplication.util.EmailBackendUtility
import com.example.myapplication.util.LocationFlow
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class AndroidPlatform(private val context: Context) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    
    init {
        WebView.setWebContentsDebuggingEnabled(true)
    }

    override fun startLocationService() {
        val intent = Intent(context, SafeWatchBackendService::class.java).apply {
            val userId = com.example.myapplication.data.remote.SupabaseManager.client.auth.currentUserOrNull()?.id
            putExtra("USER_ID", userId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun stopLocationService() {
        val intent = Intent(context, SafeWatchBackendService::class.java)
        context.stopService(intent)
    }

    override fun observeLocation(onUpdate: (Double, Double, Float) -> Unit) {
        kotlinx.coroutines.MainScope().launch {
            LocationFlow.currentLocation.collect { update ->
                update?.let { onUpdate(it.latitude, it.longitude, it.accuracy) }
            }
        }
    }

    @Composable
    override fun MapView(modifier: Modifier, latitude: Double, longitude: Double, accuracy: Float) {
        var webViewRef: WebView? by remember { mutableStateOf(null) }

        val mapHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body { height: 100%; margin: 0; padding: 0; background-color: #0B0E14; overflow: hidden; }
                    #map { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: #0B0E14; z-index: 1; }
                    .pulse { width: 18px; height: 18px; background: #007AFF; border: 2px solid white; border-radius: 50%; box-shadow: 0 0 15px #007AFF; }
                    #status-label { 
                        position: absolute; top: 15px; left: 15px; z-index: 1000;
                        color: #007AFF; font-family: sans-serif; font-size: 11px; font-weight: bold;
                        background: rgba(11, 14, 20, 0.9); padding: 8px 12px; border-radius: 8px; border: 1px solid #007AFF;
                    }
                </style>
            </head>
            <body>
                <div id="status-label">CONNECTING...</div>
                <div id="map"></div>
                <script>
                    var map; var marker; var circle; var ready = false;
                    function init() {
                        if (ready) return;
                        if (typeof L === 'undefined') {
                            setTimeout(init, 500);
                            return;
                        }
                        try {
                            map = L.map('map', { zoomControl: false, attributionControl: false }).setView([20, 0], 2);
                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
                            marker = L.marker([0, 0], { 
                                icon: L.divIcon({ className: 'u-icon', html: '<div class="pulse"></div>', iconSize: [20, 20], iconAnchor: [10, 10] }) 
                            }).addTo(map);
                            circle = L.circle([0, 0], { radius: 0, color: '#007AFF', weight: 1, fillOpacity: 0.1 }).addTo(map);
                            ready = true;
                            document.getElementById('status-label').innerText = "ENGINE READY: WAITING GPS";
                            setTimeout(function() { map.invalidateSize(); }, 300);
                        } catch(e) { document.getElementById('status-label').innerText = "JS ERR: " + e.message; }
                    }
                    function updateLocation(lat, lng, acc) {
                        if (!ready) init();
                        if (ready && map) {
                            var loc = [lat, lng];
                            marker.setLatLng(loc);
                            circle.setLatLng(loc);
                            circle.setRadius(acc || 0);
                            map.setView(loc, 16);
                            document.getElementById('status-label').style.display = 'none';
                        }
                    }
                    init();
                </script>
            </body>
            </html>
        """.trimIndent()

        LaunchedEffect(webViewRef, latitude, longitude, accuracy) {
            if (webViewRef != null && latitude != 0.0 && longitude != 0.0) {
                webViewRef?.evaluateJavascript("updateLocation($latitude, $longitude, $accuracy)", null)
            }
        }

        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#0B0E14"))
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"
                    }
                    loadDataWithBaseURL("https://safewatch.app", mapHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { view ->
                webViewRef = view
            }
        )
    }

    @Composable
    override fun HistoryMapView(modifier: Modifier, points: List<com.example.myapplication.data.model.HistoryPoint>) {
        var webViewRef: WebView? by remember { mutableStateOf(null) }

        val pointsJson = points.map { 
            "{\"lat\": ${it.latitude}, \"lng\": ${it.longitude}}" 
        }.toString()

        val mapHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body { height: 100%; margin: 0; padding: 0; background-color: #0B0E14; overflow: hidden; }
                    #map { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: #0B0E14; z-index: 1; }
                    .marker-pin { width: 12px; height: 12px; border-radius: 50%; border: 2px solid white; box-shadow: 0 0 5px rgba(0,0,0,0.5); }
                    .marker-start { background: #4CAF50; }
                    .marker-end { background: #F44336; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map; var polyline; var markers = [];
                    function init() {
                        if (typeof L === 'undefined') { setTimeout(init, 500); return; }
                        map = L.map('map', { zoomControl: false, attributionControl: false });
                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
                        updatePoints($pointsJson);
                    }
                    function updatePoints(points) {
                        if (!map) return;
                        if (polyline) map.removeLayer(polyline);
                        markers.forEach(m => map.removeLayer(m));
                        markers = [];

                        if (points.length > 0) {
                            var latlngs = points.map(p => [p.lat, p.lng]);
                            if (points.length > 1) {
                                polyline = L.polyline(latlngs, { color: '#00F2FF', weight: 4, opacity: 0.8 }).addTo(map);
                            }
                            points.forEach((p, i) => {
                                if (i === 0 || i === points.length - 1) {
                                    var className = i === 0 ? "marker-pin marker-start" : "marker-pin marker-end";
                                    var marker = L.marker([p.lat, p.lng], {
                                        icon: L.divIcon({ className: 'custom-div-icon', html: '<div class="' + className + '"></div>', iconSize: [12, 12], iconAnchor: [6, 6] })
                                    }).addTo(map);
                                    markers.push(marker);
                                }
                            });
                            var bounds = L.latLngBounds(latlngs);
                            map.fitBounds(bounds, { padding: [50, 50] });
                        }
                    }
                    init();
                </script>
            </body>
            </html>
        """.trimIndent()

        LaunchedEffect(points) {
            if (webViewRef != null && points.isNotEmpty()) {
                val json = points.map { "{\"lat\": ${it.latitude}, \"lng\": ${it.longitude}}" }.toString()
                webViewRef?.evaluateJavascript("updatePoints($json)", null)
            }
        }

        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.parseColor("#0B0E14"))
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    loadDataWithBaseURL("https://safewatch.app", mapHtml, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { view ->
                webViewRef = view
            }
        )
    }

    override fun triggerEmergencyProtocol() {
        val intent = Intent(context, SafeWatchBackendService::class.java).apply { action = "TRIGGER_SOS" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun sendNativeSms(phoneNumber: String, message: String) {
        android.util.Log.i("Platform", "📠 Sending native SMS to $phoneNumber")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("Platform", "❌ Missing SEND_SMS permission!")
            return
        }
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            android.util.Log.i("Platform", "✅ SMS message sent successfully")
        } catch (e: Exception) { 
            android.util.Log.e("Platform", "❌ SMS Transmission FAILED: ${e.message}")
        }
    }

    override fun sendEmail(to: String, subject: String, body: String) {
        EmailBackendUtility.sendOtpEmail(to, body)
    }

    @Composable
    override fun PermissionManager(onAllGranted: () -> Unit) {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                onAllGranted()
            }
        }

        LaunchedEffect(Unit) { launcher.launch(permissions) }
    }

    companion object {
        private var manualRecorder: MediaRecorder? = null
        private var lastManualFile: String? = null
        private var mediaPlayer: MediaPlayer? = null
        private var currentAudioUrl: String? = null
    }

    override fun playAudio(url: String) {
        println("PLAYBACK_REQUESTED: $url")
        try {
            stopAudio()
            mediaPlayer = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                }
                setDataSource(context, Uri.parse(url))
                setOnPreparedListener { it.start() }
                setOnErrorListener { mp, _, _ -> mp.release(); true }
                setOnCompletionListener { it.release(); mediaPlayer = null; currentAudioUrl = null }
                prepareAsync()
            }
            currentAudioUrl = url
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun toggleAudio(url: String): Boolean {
        return try {
            val player = mediaPlayer
            if (player != null && currentAudioUrl == url) {
                if (player.isPlaying) {
                    player.pause()
                    false
                } else {
                    player.start()
                    true
                }
            } else {
                playAudio(url)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentAudioUrl = null
        } catch (e: Exception) {}
    }

    override fun startManualRecording(): String? {
        return try {
            val file = java.io.File(context.filesDir, "MANUAL_REC_${System.currentTimeMillis()}.m4a")
            manualRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            lastManualFile = file.absolutePath
            println("RECORDING_STARTED: $lastManualFile")
            lastManualFile
        } catch (e: Exception) { 
            println("RECORDING_FAILED: ${e.message}")
            null 
        }
    }

    override fun stopManualRecording(): String? {
        return try {
            val recorder = manualRecorder
            if (recorder != null) {
                recorder.stop()
                recorder.release()
                manualRecorder = null
                println("RECORDING_STOPPED: $lastManualFile")
                
                val file = java.io.File(lastManualFile ?: "")
                if (file.exists()) {
                    println("LOCAL_FILE_CREATED: ${file.absolutePath}")
                    println("FILE_SIZE: ${file.length()} bytes")
                } else {
                    println("LOCAL_FILE_MISSING: $lastManualFile")
                }
            }
            lastManualFile
        } catch (e: Exception) {
            println("RECORDING_STOP_FAILED: ${e.message}")
            try { manualRecorder?.release() } catch (e2: Exception) {}
            manualRecorder = null
            lastManualFile
        }
    }

    override fun readFileBytes(path: String): ByteArray? = java.io.File(path).readBytes()

    @Composable
    override fun ImagePicker(onImagePicked: (ByteArray) -> Unit) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                val bytes = context.contentResolver.openInputStream(it)?.use { input ->
                    input.readBytes()
                }
                if (bytes != null) onImagePicked(bytes)
            }
        }
        
        var launched by remember { mutableStateOf(false) }
        
        LaunchedEffect(launched) {
            if (!launched) {
                launcher.launch("image/*")
                launched = true
            }
        }
    }

    override fun persistUserId(userId: String) {
        context.getSharedPreferences("SafeWatch", Context.MODE_PRIVATE)
            .edit()
            .putString("uid", userId)
            .apply()
    }

    override fun persistSetting(key: String, value: String) {
        context.getSharedPreferences("SafeWatch", Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }

    override fun getPersistedSetting(key: String, defaultValue: String): String {
        return context.getSharedPreferences("SafeWatch", Context.MODE_PRIVATE)
            .getString(key, defaultValue) ?: defaultValue
    }

    override fun refreshSafetyServiceSettings() {
        val intent = Intent(context, SafeWatchBackendService::class.java).apply { action = "REFRESH_SETTINGS" }
        context.startService(intent)
    }
}

lateinit var androidContext: Context
actual fun getPlatform(): Platform = AndroidPlatform(androidContext)
