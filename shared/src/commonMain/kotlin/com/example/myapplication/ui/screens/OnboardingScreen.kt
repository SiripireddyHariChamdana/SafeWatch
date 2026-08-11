package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

val onboardingPages = listOf(
    OnboardingData(
        title = "Live GPS Tracking",
        description = "Share your real-time location with your trusted emergency circle.",
        icon = Icons.Default.GpsFixed,
        accentColor = NeonBlue
    ),
    OnboardingData(
        title = "Safety Check-in",
        description = "Set a timer and let SafeWatch alert your family if you don't check in.",
        icon = Icons.Default.Timer,
        accentColor = NeonPurple
    ),
    OnboardingData(
        title = "Instant SOS",
        description = "One tap to broadcast an emergency alert with your live path.",
        icon = Icons.Default.Warning,
        accentColor = Color.Red
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { onboardingPages.size }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkNavy, Color.Black)))
    ) {
        SecurityGridBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // Skip
            Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onFinish) {
                    Text("Skip", color = Color.White.copy(alpha = 0.5f))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                val data = onboardingPages[pageIndex]
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(150.dp).blur(40.dp).background(data.accentColor.copy(alpha = 0.2f), CircleShape))
                        Icon(data.icon, null, tint = data.accentColor, modifier = Modifier.size(100.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Text(data.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(data.description, color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
                }
            }

            // Bottom Nav
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(onboardingPages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) onboardingPages[pagerState.currentPage].accentColor else Color.White.copy(alpha = 0.2f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = onboardingPages[pagerState.currentPage].accentColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (pagerState.currentPage == onboardingPages.size - 1) "Get Started" else "Continue", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
