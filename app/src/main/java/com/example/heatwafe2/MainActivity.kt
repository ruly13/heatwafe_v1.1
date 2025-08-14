package com.example.heatwafe2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // <-- IMPORT BARU
import androidx.compose.foundation.verticalScroll     // <-- IMPORT BARU
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Surface
import com.example.heatwafe2.ui.theme.HeatWafeTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.valentinilk.shimmer.shimmer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            HeatWafeTheme {
                LayarUtama()
            }
        }
    }
}

@Composable
fun LayarUtama() {
    var rataSuhu by remember { mutableStateOf("") }
    var rataKelembaban by remember { mutableStateOf("") }
    var Ele by remember { mutableStateOf("") }
    var curPower by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance()
    val ref = database.reference

    // Gunakan DisposableEffect untuk listener Firebase
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rataSuhu = snapshot.child("rata_suhu").value?.toString() ?: "N/A"
                rataKelembaban = snapshot.child("rata_kelembaban").value?.toString() ?: "N/A"
                Ele = snapshot.child("total_ele").value?.toString() ?: "N/A"
                curPower = snapshot.child("cur_power").value?.toString() ?: "N/A"
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Gagal mengambil data sensor: ${error.message}", Toast.LENGTH_SHORT).show()
                isLoading = false // Pastikan loading false jika error
            }
        }
        val sensorRef = ref.child("sensor")
        sensorRef.addValueEventListener(listener)

        onDispose {
            sensorRef.removeEventListener(listener) // Hapus listener saat keluar
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val scrollState = rememberScrollState() // <-- State untuk scroll

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // Hapus verticalArrangement = Arrangement.SpaceBetween
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState) // <-- TAMBAHKAN INI
        ) {
            // Header
            KolomHeader()

            Spacer(modifier = Modifier.height(24.dp)) // Beri spasi

            // Bagian Sensor
            KolomSensor(rataSuhu, rataKelembaban, Ele, curPower, isLoading)

            Spacer(modifier = Modifier.height(24.dp)) // Beri spasi

            // Bagian Kontrol

            KolomKontrol()

            Spacer(modifier = Modifier.height(32.dp)) // Beri spasi sebelum tombol

            // Tombol History (sekarang akan terlihat setelah scroll)
            KolomTombolHistory(
                onSimpanClick = {
                    val kunciLogBaru = ref.child("logs").push().key ?: return@KolomTombolHistory
                    val refLog = ref.child("logs").child(kunciLogBaru) // Simpan di root log, bukan 'sensor'

                    val timestamp = dapatkanTimestampSekarang()

                    val data = mapOf(
                        "timestamp" to timestamp,
                        "rata_suhu" to rataSuhu.toDoubleOrNull(),
                        "rata_kelembaban" to rataKelembaban.toDoubleOrNull(),
                        "ele" to Ele.toDoubleOrNull(),
                        "cur_power" to curPower.toDoubleOrNull()
                    )

                    refLog.setValue(data)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                        }
                },
                onLihatClick = {
                    context.startActivity(Intent(context, HistoryActivity::class.java))
                }
            )

            Spacer(modifier = Modifier.height(16.dp)) // Spasi di bagian bawah
        }
    }
}
@Composable
fun KolomTombolHistory(
    onSimpanClick: () -> Unit,
    onLihatClick: () -> Unit,
    pembatasTebal: Dp = 1.dp,
    pembatasWarna: Color = Color.Gray
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onSimpanClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("💾 Simpan")
        }

        // Pembatas
        VerticalDivider( // <-- Pastikan VerticalDivider diimpor dengan benar
            color = pembatasWarna,
            thickness = pembatasTebal,
            modifier = Modifier.height(36.dp) // Sesuaikan tinggi pembatas agar pas
        )

        Button(
            onClick = onLihatClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("📋 Log")
        }
    }
}


@Composable
private fun KolomHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // Hapus verticalArrangement dan fillMaxHeight
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "HeatWafe",
                style = MaterialTheme.typography.displayLarge, // Mungkin displayMedium atau displaySmall lebih pas?
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "v1.1",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black // Sebaiknya gunakan MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = painterResource(id = R.drawable.logo_saya),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp) // <-- PERKECIL UKURAN GAMBAR
        )
    }
}

// --- KolomSensor, KolomKontrol, ItemSensor, ItemShimmer, dapatkanTimestampSekarang ---
// (Tetap sama seperti kode Anda sebelumnya, tidak perlu diubah untuk scrolling)
// ... (Salin kode KolomSensor, KolomKontrol, dll. ke sini) ...

@Composable
private fun KolomSensor(
    rataSuhu: String,
    rataKelembaban: String,
    addEle: String,
    curPower: String,
    isLoading: Boolean
) {
    // Animasi nilai sensor
    val suhuAnimasi by animateFloatAsState(
        targetValue = rataSuhu.toFloatOrNull() ?: 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    val kelembabanAnimasi by animateFloatAsState(
        targetValue = rataKelembaban.toFloatOrNull() ?: 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    val energiAnimasi by animateFloatAsState(
        targetValue = addEle.toFloatOrNull() ?: 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    val dayaAnimasi by animateFloatAsState(
        targetValue = curPower.toFloatOrNull() ?: 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Sensor Real-time",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(2) { ItemShimmer() }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(2) { ItemShimmer() }
                }
            }
        } else {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ItemSensor("🌡️", "Suhu", String.format(Locale.US, "%.2f°C", suhuAnimasi), MaterialTheme.colorScheme.primary)
                    ItemSensor("💧", "Kelembaban", String.format(Locale.US, "%.2f%%", kelembabanAnimasi), MaterialTheme.colorScheme.primary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ItemSensor("⚡", "Energi", String.format(Locale.US, "%.2f kWh", energiAnimasi), MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun KolomKontrol() {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().getReference("kontrol")
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var switchState by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(true) }

    // Dengarkan perubahan nilai switch_1 secara realtime
    LaunchedEffect(Unit) {
        database.child("switch_1").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Boolean::class.java)
                if (value != null) {
                    switchState = value
                    isOnline = true
                } else {
                    isOnline = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                isOnline = false
                Toast.makeText(context, "Koneksi Firebase gagal: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            // Tombol MATIKAN DARURAT
            Button(
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        database.child("switch_1").setValue(false).addOnCompleteListener {
                            isLoading = false
                            val msg = if (it.isSuccessful) "Mode Darurat Diaktifkan!" else "Gagal mematikan sistem!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp),
                enabled = !isLoading && isOnline && switchState
            ) {
                if (isLoading && switchState) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PowerOff, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MATIKAN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol MATIKAN
            Button(
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        database.child("switch_1").setValue(true).addOnCompleteListener {
                            isLoading = false
                            val msg = if (it.isSuccessful) "Mode Direset!" else "Gagal mereset sistem!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp),
                enabled = !isLoading && isOnline && !switchState
            ) {
                if (isLoading && !switchState) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESET", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hanya tampilkan jika perangkat offline
            if (!isOnline) {
                Text(
                    "PERANGKAT OFFLINE",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ItemSensor(ikon: String, label: String, nilai: String, warna: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .padding(8.dp)
            .width(150.dp) // <-- Perlebar sedikit agar muat
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = ikon, fontSize = 24.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = warna)
            Text(text = nilai, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ItemShimmer() {
    Card( // Gunakan Card agar ukurannya konsisten
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(8.dp)
            .width(150.dp)
            .height(100.dp)
            .shimmer(),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
    ){
        // Box kosong untuk efek shimmer
    }
}

fun dapatkanTimestampSekarang(): String {
    // Format ISO 8601 lebih standar untuk timestamp
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC") // Gunakan UTC untuk konsistensi
    return sdf.format(Date())
}