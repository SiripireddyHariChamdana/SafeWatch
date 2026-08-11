package com.example.myapplication.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import com.example.myapplication.MainActivity

class FakeCallReceiver : BroadcastReceiver() {
    companion object {
        private var mediaPlayer: MediaPlayer? = null
        private var vibrator: Vibrator? = null

        fun stopFakeCall() {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                vibrator?.cancel()
                vibrator = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Play Ringtone
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer.create(context, ringtoneUri)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            
            // 2. Vibrate
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 1000, 1000)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
            }
            
            // 3. Bring App to Foreground
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("SHOW_FAKE_CALL", true)
            }
            context.startActivity(launchIntent)
            
            println("✅ FAKE CALL TRIGGERED")
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
