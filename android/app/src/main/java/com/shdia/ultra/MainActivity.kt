package com.shdia.ultra

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val BASE_URL = "http://10.0.2.2:8000" // Emulador. Para celular real, use o IP/HTTPS do servidor.

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShdiaApp() }
    }
}

@Composable
private fun ShdiaApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dark = Color(0xFF101018)
    val panel = Color(0xFF1C1C29)
    val accent = Color(0xFF9B87FF)
    var selectedStyle by remember { mutableStateOf("anime") }
    var upscale by remember { mutableStateOf("2x") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Escolha uma imagem para começar.") }
    var jobId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
        jobId = null
        if (uri != null) status = "Imagem selecionada. Envie para a IA na nuvem."
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = accent, background = dark, surface = panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Column(
            Modifier.fillMaxSize().background(dark).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("SHDIA ULTRA", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("AI ENHANCER • CLOUD FIRST", fontSize = 10.sp, color = accent, letterSpacing = 1.4.sp)
                }
            }
            Text("Escolha seu estilo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StyleCard("🎌 Anime", selectedStyle == "anime", panel, accent) { selectedStyle = "anime" }
                StyleCard("🎬 Live Action", selectedStyle == "live_action", panel, accent) { selectedStyle = "live_action" }
                StyleCard("✨ Restaurar", selectedStyle == "restore", panel, accent) { selectedStyle = "restore" }
            }
            Card(colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = accent, modifier = Modifier.size(38.dp))
                    Text("Melhoria com IA na nuvem", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("As funções avançadas exigem internet. A API precisa ter um modelo de IA real conectado para produzir a imagem final.", color = Color.LightGray, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ampliação", modifier = Modifier.weight(1f))
                        FilterChip(selected = upscale == "2x", onClick = { upscale = "2x" }, label = { Text("2×") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = upscale == "4x", onClick = { upscale = "4x" }, label = { Text("4×") })
                    }
                    OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedUri == null) "Selecionar imagem" else "Trocar imagem")
                    }
                    Button(
                        onClick = {
                            val uri = selectedUri ?: return@Button
                            busy = true
                            status = "Enviando arquivo para a nuvem…"
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        uploadImage(context, uri, selectedStyle, upscale)
                                    }
                                    jobId = result.optString("job_id")
                                    status = result.optString("message", "Arquivo enviado.")
                                } catch (e: Exception) {
                                    status = "Não foi possível conectar. Verifique a internet e o endereço da API."
                                } finally { busy = false }
                            }
                        },
                        enabled = selectedUri != null && !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (busy) "Enviando…" else "Enviar para IA")
                    }
                    Text(status, color = if (jobId != null) accent else Color.LightGray, fontSize = 13.sp)
                    if (jobId != null) Text("ID do trabalho: $jobId", fontSize = 11.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("SHDIA ULTRA v2 • Protótipo de desenvolvimento", color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StyleCard(label: String, selected: Boolean, panel: Color, accent: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) accent.copy(alpha = 0.22f) else panel),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, accent) else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), fontWeight = FontWeight.SemiBold)
    }
}

private fun uploadImage(
    context: android.content.Context,
    uri: Uri,
    style: String,
    upscale: String
): JSONObject {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IOException("Não foi possível ler a imagem.")
    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("style", style)
        .addFormDataPart("upscale", upscale)
        .addFormDataPart("file", "shdia_upload", bytes.toRequestBody(mime.toMediaTypeOrNull()))
        .build()
    val request = Request.Builder().url("$BASE_URL/jobs").post(body).build()
    val client = OkHttpClient.Builder().callTimeout(90, TimeUnit.SECONDS).build()
    client.newCall(request).execute().use { response ->
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw IOException("API retornou ${response.code}: $text")
        return JSONObject(text)
    }
}
