package com.tejashaqua.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.data.model.AquaRate
import com.tejashaqua.app.data.model.RateTrend
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateGraphBottomSheet(rate: AquaRate, onDismiss: () -> Unit) {
    var historicalData by remember { mutableStateOf<List<Double>>(emptyList()) }
    var historicalLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    LaunchedEffect(rate.name) {
        db.collection("aqua_rates").document(rate.name)
            .collection("history")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .limitToLast(7)
            .get()
            .addOnSuccessListener { snapshot ->
                val data = mutableListOf<Double>()
                val labels = mutableListOf<String>()
                val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())

                if (snapshot.isEmpty) {
                    // Fallback to simple logic if no history exists yet
                    val currentPriceStr = rate.price.filter { it.isDigit() || it == '.' }
                    val currentPrice = currentPriceStr.toDoubleOrNull() ?: 150.0
                    
                    val calendar = Calendar.getInstance()
                    for (i in 6 downTo 0) {
                        val pastCalendar = Calendar.getInstance()
                        pastCalendar.time = calendar.time
                        pastCalendar.add(Calendar.DATE, -i)
                        labels.add(sdf.format(pastCalendar.time))
                        data.add(currentPrice) // Horizontal line if no history
                    }
                } else {
                    snapshot.documents.forEach { doc ->
                        val price = doc.getDouble("price") ?: 0.0
                        val ts = doc.getLong("timestamp") ?: 0L
                        if (price > 0) {
                            data.add(price)
                            labels.add(sdf.format(Date(ts)))
                        }
                    }
                }
                
                historicalData = data
                historicalLabels = labels
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = rate.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "7 Days Rate Trend", fontSize = 14.sp, color = GrayText)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AquaBlue)
                }
            } else {
                RateTrendGraph(
                    data = historicalData,
                    labels = historicalLabels,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Current Rate", fontSize = 12.sp, color = GrayText)
                    Text(text = rate.price, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Change", fontSize = 12.sp, color = GrayText)
                    Text(
                        text = rate.change, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (rate.trend == RateTrend.UP) Color(0xFF4CAF50) else if (rate.trend == RateTrend.DOWN) Color(0xFFF44336) else GrayText
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
