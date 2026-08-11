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
import com.example.myapplication.service.FakeCallReceiver
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

    override fun scheduleFakeCall(secondsFromNow: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FakeCallReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val triggerTime = SystemClock.elapsedRealtime() + (secondsFromNow * 1000)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
    }

    override fun triggerEmergencyProtocol() {
        val intent = Intent(context, SafeWatchBackendService::class.java).apply { action = "TRIGGER_SOS" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun stopFakeCallMedia() {
        FakeCallReceiver.stopFakeCall()
    }

    override fun sendNativeSms(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
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
        } catch (e: Exception) { }
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

    override fun playAudio(url: String) {
        println("PLAYBACK_REQUESTED: $url")
        try {
            val mediaPlayer = MediaPlayer()
            
            // Set audio attributes for media playback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaPlayer.setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            }
            
            mediaPlayer.setDataSource(url)
            
            mediaPlayer.setOnPreparedListener { 
                println("PLAYBACK_STARTED")
                it.start() 
            }
            
            mediaPlayer.setOnErrorListener { mp, what, extra ->
                println("PLAYBACK_FAILED: Error code ($what, $extra)")
                mp.release()
                true
            }
            
            mediaPlayer.setOnCompletionListener { 
                println("PLAYBACK_COMPLETED")
                it.release() 
            }
            
            println("🔊 Audio: Preparing stream...")
            mediaPlayer.prepareAsync()
        } catch (e: Exception) {
            println("PLAYBACK_FAILED: ${e.message}")
        }
    }

    companion object {
        private var manualRecorder: MediaRecorder? = null
        private var lastManualFile: String? = null
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
        
        SideEffect {
            launcher.launch("image/*")
        }
    }

    override fun persistUserId(userId: String) {
        context.getSharedPreferences("SafeWatch", Context.MODE_PRIVATE)
            .edit()
            .putString("uid", userId)
            .apply()
    }

    override fun refreshSafetyServiceSettings() {
        val intent = Intent(context, SafeWatchBackendService::class.java).apply { action = "REFRESH_SETTINGS" }
        context.startService(intent)
    }
}

lateinit var androidContext: Context
actual fun getPlatform(): Platform = AndroidPlatform(androidContext)
