package com.avtpro.translator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.avtpro.translator.data.api.RetrofitClient
import com.avtpro.translator.data.model.SUPPORTED_LANGUAGES
import com.avtpro.translator.data.model.VoiceGender
import com.avtpro.translator.data.model.VoiceMode
import com.avtpro.translator.data.model.languageName
import com.avtpro.translator.ui.components.PrimaryButton
import com.avtpro.translator.ui.theme.SilverText
import com.avtpro.translator.util.copyUriToCache
import com.avtpro.translator.util.saveResponseBodyToFile
import com.avtpro.translator.util.shareFile
import com.avtpro.translator.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
import android.net.Uri

@Composable
fun AudioDubbingScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsViewModel.settings.collectAsState()

    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioName by remember { mutableStateOf<String?>(null) }
    var targetLanguage by remember { mutableStateOf("vi") }
    var voiceMode by remember { mutableStateOf(VoiceMode.NATURAL) }
    var voiceGender by remember { mutableStateOf(VoiceGender.FEMALE) }
    var format by remember { mutableStateOf("mp3") }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var exportedFile by remember { mutableStateOf<File?>(null) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var voiceSimilarityConsented by remember { mutableStateOf(false) }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Xác nhận quyền sử dụng giọng nói") },
            text = {
                Text(
                    "Voice Similarity phân tích đặc điểm giọng nói gốc để tạo " +
                        "giọng dịch tương tự. Chỉ sử dụng khi bạn có quyền và " +
                        "sự đồng ý hợp lệ của người nói. Nếu không đồng ý, " +
                        "ứng dụng sẽ KHÔNG gửi voice_similarity=true."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        voiceSimilarityConsented = true
                        showConsentDialog = false
                    }
                ) { Text("Tôi có quyền và đồng ý") }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text("Không đồng ý")
                }
            }
        )
    }

    val pickAudio = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        audioUri = uri
        audioName = uri?.lastPathSegment ?: "audio"
        status = if (uri != null) "Đã chọn file âm thanh." else ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Audio Dubbing", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Chọn file âm thanh, backend nhận diện ngôn ngữ, dịch và tạo " +
                "giọng đọc AI. Không có backend thì màn hình này báo lỗi thật, " +
                "không tạo file giả.",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )

        PrimaryButton(text = "Chọn file âm thanh", onClick = { pickAudio.launch("audio/*") })
        Text(audioName ?: "Chưa chọn file", color = SilverText)

        Text("Ngôn ngữ đích", style = MaterialTheme.typography.titleMedium)
        SUPPORTED_LANGUAGES.take(11).forEach { language ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = targetLanguage == language.code,
                    onClick = { targetLanguage = language.code }
                )
                Text(language.nameVi)
            }
        }

        Text("Giọng đọc", style = MaterialTheme.typography.titleMedium)
        VoiceGender.values().forEach { gender ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = voiceGender == gender,
                    onClick = { voiceGender = gender }
                )
                Text(gender.labelVi)
            }
        }
        VoiceMode.values().forEach { mode ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = voiceMode == mode,
                    onClick = { voiceMode = mode }
                )
                Text(mode.labelVi)
            }
        }
        Text(
            "Voice Similarity ở đây cũng mở dialog xác nhận quyền trước khi " +
                "gửi, giống màn hình Video Translation. Không đồng ý thì " +
                "không gửi voice_similarity=true.",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )

        Text("Định dạng xuất", style = MaterialTheme.typography.titleMedium)
        listOf("mp3", "wav").forEach { candidate ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = format == candidate,
                    onClick = { format = candidate }
                )
                Text(candidate.uppercase())
            }
        }

        if (status.isNotBlank()) Text(status, color = SilverText)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        PrimaryButton(
            text = "Tạo giọng nói AI",
            enabled = audioUri != null && !isWorking,
            onClick = {
                val uri = audioUri ?: return@PrimaryButton
                if (voiceMode == VoiceMode.VOICE_SIMILARITY && !voiceSimilarityConsented) {
                    showConsentDialog = true
                    return@PrimaryButton
                }
                isWorking = true
                error = null
                status = "Đang gửi file lên backend…"
                scope.launch {
                    try {
                        val api = RetrofitClient.create(settings.backendUrl)
                        val localFile = withContext(Dispatchers.IO) {
                            copyUriToCache(
                                context, uri, "dub_${UUID.randomUUID()}"
                            )
                        }
                        val part = MultipartBody.Part.createFormData(
                            "file", localFile.name,
                            localFile.asRequestBody("audio/*".toMediaTypeOrNull())
                        )
                        // Step 1: transcribe (language detection is server-side).
                        val transcribed = api.transcribeAudio(part)
                        status = "Đã nhận diện: " +
                            (transcribed.language?.let { languageName(it) } ?: "Auto") +
                            ". Đang tạo giọng đọc…"
                        // Step 2: dub. Voice Similarity reaches this point
                        // only after the consent dialog above was confirmed;
                        // without consent the click handler returns early.
                        val effectiveMode = voiceMode
                        val dub = api.dubAudio(
                            file = part,
                            targetLanguage = targetLanguage.toRequestBody(
                                "text/plain".toMediaTypeOrNull()
                            ),
                            voiceMode = effectiveMode.apiValue.toRequestBody(
                                "text/plain".toMediaTypeOrNull()
                            ),
                            voiceSimilarity = (
                                effectiveMode == VoiceMode.VOICE_SIMILARITY &&
                                    voiceSimilarityConsented
                                ).toString().toRequestBody(
                                "text/plain".toMediaTypeOrNull()
                            ),
                            voiceGender = voiceGender.name.lowercase().toRequestBody(
                                "text/plain".toMediaTypeOrNull()
                            ),
                            format = format.toRequestBody(
                                "text/plain".toMediaTypeOrNull()
                            )
                        )
                        val audioUrl = dub.audioUrl
                        if (audioUrl.isNullOrBlank()) {
                            // If the backend only returns a job, poll it like video.
                            val jobId = dub.jobId
                            if (jobId.isNullOrBlank()) {
                                throw IllegalStateException(
                                    "Backend không trả về audio_url hay job_id."
                                )
                            }
                            var resolvedUrl: String? = null
                            for (attempt in 0 until 150) {
                                val job = api.getJob(jobId)
                                if (job.isCompleted) {
                                    val result = api.getResult(jobId)
                                    resolvedUrl = result.videoUrl
                                    break
                                }
                                if (job.isFailed) {
                                    throw IllegalStateException(
                                        job.error ?: "Xử lý audio thất bại."
                                    )
                                }
                                kotlinx.coroutines.delay(2000L)
                            }
                            val url = resolvedUrl
                                ?: throw IllegalStateException(
                                    "Hết thời gian chờ job audio."
                                )
                            val outDir = File(context.filesDir, "results").apply { mkdirs() }
                            val out = File(outDir, "dub_${UUID.randomUUID()}.$format")
                            withContext(Dispatchers.IO) {
                                val body = api.downloadFile(
                                    RetrofitClient.resolveUrl(settings.backendUrl, url)
                                )
                                saveResponseBodyToFile(body, out)
                            }
                            exportedFile = out
                        } else {
                            val outDir = File(context.filesDir, "results").apply { mkdirs() }
                            val out = File(outDir, "dub_${UUID.randomUUID()}.$format")
                            withContext(Dispatchers.IO) {
                                val body = api.downloadFile(
                                    RetrofitClient.resolveUrl(settings.backendUrl, audioUrl)
                                )
                                saveResponseBodyToFile(body, out)
                            }
                            exportedFile = out
                        }
                        if (settings.autoDeleteTemp) localFile.delete()
                        status = "Hoàn thành. File đã lưu trong app."
                    } catch (e: Exception) {
                        error = "Không tạo được giọng đọc: ${e.message ?: e.javaClass.simpleName}. " +
                            "Kiểm tra backend: ${settings.backendUrl}"
                    } finally {
                        isWorking = false
                    }
                }
            }
        )

        exportedFile?.let { file ->
            PrimaryButton(
                text = "Chia sẻ file ${format.uppercase()}",
                onClick = {
                    shareFile(
                        context, file,
                        if (format == "wav") "audio/wav" else "audio/mpeg",
                        "Chia sẻ audio đã lồng tiếng"
                    )
                }
            )
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Quay lại")
        }
    }
}
