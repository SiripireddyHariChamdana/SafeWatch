package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@Composable
fun AnalyticsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))
            
            Text("Safety Insights", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Your activity overview for the last 30 days", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            // STATS GRID
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnalyticsCard("Safety Score", "98%", NeonBlue, Modifier.weight(1f))
                AnalyticsCard("Safe Zones", "12", NeonPurple, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnalyticsCard("Distance", "142km", Color.Green, Modifier.weight(1f))
                AnalyticsCard("SOS Alerts", "0", Color.Red, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CHART PLACEHOLDER
            Text("Weekly Activity", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Safety Trend Visualization", color = NeonBlue.copy(alpha = 0.4f), fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // HISTORY SUMMARY
            Text("Recent Summary", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            SummaryItem("Active Tracking", "Daily commute tracked", Color.Green)
            SummaryItem("Check-in Timer", "Used 5 times this week", NeonPurple)
            SummaryItem("Battery Alert", "Optimized location sharing", NeonBlue)
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun AnalyticsCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun SummaryItem(title: String, desc: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}
