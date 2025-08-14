package com.example.heatwafe2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.heatwafe2.ui.theme.HeatWafeTheme
import com.google.firebase.database.*
import java.text.NumberFormat // <-- IMPORT YANG HILANG SUDAH DITAMBAHKAN
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

// --- KONSTANTA YANG HILANG SUDAH DITAMBAHKAN ---
private const val HARGA_PER_KWH = 1500.0 // Harga Rp 1500 per kWh

data class SensorLog(
    val id: String = "",
    val time: String = "",
    val rataSuhu: String = "",
    val rataKelembaban: String = "",
    val addEle: String = "",
)

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HeatWafeTheme {
                HistoryScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text("Log Sensor", fontWeight = FontWeight.Bold) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        LogScreen()
    }
}

@Composable
fun LogScreen() {
    val logList = remember { mutableStateListOf<SensorLog>() }
    val dbRef = FirebaseDatabase.getInstance().getReference("logs")

    fun deleteLog(logId: String) {
        if (logId.isNotEmpty()) {
            dbRef.child(logId).removeValue()
        }
    }

    LaunchedEffect(Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<SensorLog>()
                snapshot.children.forEach { logEntry ->
                    val log = SensorLog(
                        id = logEntry.key ?: "",
                        time = logEntry.child("timestamp").getValue(String::class.java) ?: "",
                        rataSuhu = "${logEntry.child("rata_suhu").getValue(Double::class.java) ?: 0.0}°C",
                        rataKelembaban = "${logEntry.child("rata_kelembaban").getValue(Double::class.java) ?: 0.0}%",
                        addEle = "${logEntry.child("ele").getValue(Double::class.java) ?: 0.0} kWh")
                    if (log.time.isNotEmpty()) {
                        tempList.add(log)
                    }
                }
                val sortedList = tempList.sortedByDescending { log ->
                    try {
                        OffsetDateTime.parse(log.time)
                    } catch (e: DateTimeParseException) {
                        OffsetDateTime.MIN
                    }
                }
                logList.clear()
                logList.addAll(sortedList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("HistoryActivity", "Failed to read value.", error.toException())
            }
        })
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(logList, key = { it.id }) { log ->
            SensorCard(
                log = log,
                onDelete = { deleteLog(log.id) }
            )
        }
    }
}

@Composable
fun SensorCard(log: SensorLog, onDelete: () -> Unit) {
    val iconMap = mapOf(
        "Suhu" to Icons.Filled.Thermostat,
        "Kelembaban" to Icons.Filled.Waves,
        "KWH" to Icons.Filled.Bolt,
        "Daya" to Icons.Filled.ElectricBolt,
        "Biaya" to Icons.Filled.MonetizationOn
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val formattedTime = try {
                val utcTime = OffsetDateTime.parse(log.time)
                val localTime = utcTime.atZoneSameInstant(ZoneId.systemDefault())
                val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
                localTime.format(outputFormatter)
            } catch (e: DateTimeParseException) {
                "Waktu Tidak Valid"
            }

            val formattedBiaya = try {
                val kwhValue = log.addEle.removeSuffix(" kWh").trim().toDoubleOrNull() ?: 0.0
                val totalBiaya = kwhValue * HARGA_PER_KWH
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                formatter.format(totalBiaya)
            } catch (e: Exception) {
                "Rp 0"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Waktu: $formattedTime", style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Log",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            SensorRow(iconMap["Suhu"]!!, "Suhu", log.rataSuhu, Color.Red)
            SensorRow(iconMap["Kelembaban"]!!, "Kelembaban", log.rataKelembaban, Color(0xFF1976D2))
            SensorRow(iconMap["KWH"]!!, "Energi (kWh)", log.addEle, Color(0xFF6A1B9A))
            SensorRow(iconMap["Biaya"]!!, "Estimasi Biaya", formattedBiaya, Color(0xFFF9A825))
        }
    }
}

@Composable
fun SensorRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = color)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = value, color = color, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun SensorCardPreview() {
    val sampleLog = SensorLog(
        id = "sample-id-123",
        time = "2025-06-19T16:32:00Z",
        rataSuhu = "25.5°C",
        rataKelembaban = "60.0%",
        addEle = "1.23 kWh",
    )
    HeatWafeTheme {
        SensorCard(
            log = sampleLog,
            onDelete = {}
        )
    }
}