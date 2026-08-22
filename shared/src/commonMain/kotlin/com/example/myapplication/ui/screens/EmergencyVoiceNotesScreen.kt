package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.data.remote.SupabaseManager
import com.example.myapplication.getPlatform
import com.example.myapplication.data.model.Evidence
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyVoiceNotesScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var recordings by remember { mutableStateOf<List<Evidence>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchRecordings() {
        isLoading = true
        scope.launch {
            try {
                val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return@launch
                println("🚀 VoiceNotes: Fetching recordings for $userId")
                val result = SupabaseManager.client.postgrest["evidence_vault"]
                    .select(columns = Columns.ALL) {
                        filter { eq("user_id", userId) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Evidence>()
                recordings = result
                println("✅ VoiceNotes: Found ${result.size} recordings")
            } catch (e: Exception) {
                println("❌ VoiceNotes: Fetch FAILED: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteRecording(recording: Evidence) {
        scope.launch {
            try {
                // 1. Delete from Storage
                SupabaseManager.client.storage.from("voice-recordings").delete(listOf(recording.storage_path))
                // 2. Delete from Database
                SupabaseManager.client.postgrest["evidence_vault"].delete {
                    filter { eq("id", recording.id.toString()) }
                }
                fetchRecordings()
                println("✅ VoiceNotes: Deleted ${recording.file_name}")
            } catch (e: Exception) {
                println("❌ VoiceNotes: Delete FAILED: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchRecordings()
    }

    var isRecording by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var currentPlayingUrl by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Voice Recordings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (isRecording || isUploading || currentPlayingUrl != null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = if (isRecording) Color.Red else NeonBlue,
                    trackColor = Color.Transparent
                )
                Text(
                    text = when {
                        isRecording -> "RECORDING_STARTED: Capturing evidence..."
                        isUploading -> "UPLOAD_STARTED: Syncing to cloud..."
                        else -> if (isAudioPlaying) "PLAYBACK_ACTIVE" else "PLAYBACK_PAUSED"
                    },
                    color = if (isRecording) Color.Red else NeonBlue,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (isLoading && !isUploading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No recordings in your vault.", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recordings) { recording ->
                        val url = SupabaseManager.getPlayableUrl(recording.storage_path)
                        EvidenceCard(
                            fileName = recording.file_name,
                            date = recording.created_at?.let { formatTime(it) } ?: "Date N/A",
                            isPlaying = currentPlayingUrl == url && isAudioPlaying,
                            onPlay = {
                                if (currentPlayingUrl != url) {
                                    getPlatform().stopAudio()
                                    currentPlayingUrl = url
                                }
                                isAudioPlaying = getPlatform().toggleAudio(url)
                            },
                            onDelete = { deleteRecording(recording) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Recording FAB
        FloatingActionButton(
            onClick = {
                if (isUploading) return@FloatingActionButton
                
                if (!isRecording) {
                    val path = getPlatform().startManualRecording()
                    if (path != null) {
                        isRecording = true
                        println("🎙️ VoiceNotes: Recording started at $path")
                    }
                } else {
                    val path = getPlatform().stopManualRecording()
                    isRecording = false
                    println("🎙️ VoiceNotes: Recording stopped at $path")
                    
                    if (path != null) {
                        isUploading = true
                        scope.launch {
                            val userId = SupabaseManager.client.auth.currentUserOrNull()?.id
                            if (userId != null) {
                                val bytes = getPlatform().readFileBytes(path)
                                if (bytes != null && bytes.isNotEmpty()) {
                                    val timestamp = Clock.System.now().toEpochMilliseconds()
                                    val isoNow = kotlinx.datetime.Clock.System.now().toString()
                                    val name = "Manual_Note_$timestamp.m4a"
                                    println("🚀 VoiceNotes: Uploading $name...")
                                    
                                    SupabaseManager.uploadEvidence(userId, name, bytes, isoNow)
                                    println("✅ VoiceNotes: Sync initiated via Java")
                                    fetchRecordings()
                                } else {
                                    println("⚠️ VoiceNotes: File is empty or null")
                                }
                            }
                            isUploading = false
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = if (isRecording) Color.Red else NeonBlue,
            shape = CircleShape
        ) {
            if (isUploading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic, 
                    null, 
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun EvidenceCard(fileName: String, date: String, isPlaying: Boolean = false, onPlay: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).clickable { onPlay() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) NeonBlue.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isPlaying) NeonBlue else NeonBlue.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(
                    if (isPlaying) NeonBlue else NeonBlue.copy(alpha = 0.1f)
                ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(Icons.Default.Pause, null, tint = Color.White)
                } else {
                    Icon(Icons.Default.PlayArrow, null, tint = NeonBlue)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName, 
                    color = Color.White, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1
                )
                Text(
                    text = if (isPlaying) "Playing..." else "Recorded on $date", 
                    color = Color.White.copy(alpha = 0.5f), 
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = null, 
                    tint = NeonBlue
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = RedSOS.copy(alpha = 0.7f))
            }
        }
    }
}

private fun formatTime(iso: String): String {
    return try {
        val sanitized = iso.replace(" ", "T")
        val instant = Instant.parse(sanitized)
        val lt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = if (lt.hour % 12 == 0) 12 else lt.hour % 12
        val ampm = if (lt.hour < 12) "AM" else "PM"
        
        val month = lt.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        "${lt.dayOfMonth} $month ${lt.year}, ${hour}:${lt.minute.toString().padStart(2, '0')} $ampm"
    } catch (e: Exception) {
        iso.take(16)
    }
}
