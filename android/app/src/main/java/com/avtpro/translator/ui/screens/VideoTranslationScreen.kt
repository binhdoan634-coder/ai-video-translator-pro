package com.avtpro.translator.ui.screens

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.avtpro.translator.data.model.Language
import com.avtpro.translator.data.model.SUPPORTED_LANGUAGES
import com.avtpro.translator.data.model.VoiceGender
import com.avtpro.translator.data.model.VoiceMode
import com.avtpro.translator.data.model.languageName
import com.avtpro.translator.data.settings.AppSettings
import com.avtpro.translator.navigation.Routes
import com.avtpro.translator.ui.components.PrimaryButton
import com.avtpro.translator.ui.components.ToggleRow
import com.avtpro.translator.ui.theme.NavySurface
import com.avtpro.translator.ui.theme.SilverText
import com.avtpro.translator.util.formatDuration
import com.avtpro.translator.util.formatFileSize
import com.avtpro.translator.util.formatResolution
import com.avtpro.translator.viewmodel.TranslationUiState
import com.avtpro.translator.viewmodel.TranslationViewModel

@Composable
fun VideoTranslationScreen(
    settings: AppSettings,
    translationViewModel: TranslationViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by translationViewModel.state.collectAsState()

    var targetLanguage by remember { mutableStateOf(translationViewModel.targetLanguage) }
    var voiceMode by remember { mutableStateOf(translationViewModel.voiceMode) }
    var voiceGender by remember { mutableStateOf(translationViewModel.voiceGender) }
    var subtitleEnabled by remember { mutableStateOf(translationViewModel.subtitleEnabled) }
    var backgroundMusic by remember { mutableStateOf(translationViewModel.backgroundMusic) }
    var keepOriginalAudio by remember { mutableStateOf(translationViewModel.keepOriginalAudio) }
    var bilingualSubtitle by remember { mutableStateOf(translationViewModel.bilingualSubtitle) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }

    val uri = translationViewModel.selectedVideoUri
    val metadata = translationViewModel.selectedVideoMetadata

    fun applyAndStart() {
        translationViewModel.targetLanguage = targetLanguage
        translationViewModel.voiceMode = voiceMode
        translationViewModel.voiceGender = voiceGender
        translationViewModel.subtitleEnabled = subtitleEnabled
        translationViewModel.backgroundMusic = backgroundMusic
        translationViewModel.keepOriginalAudio = keepOriginalAudio
        translationViewModel.bilingualSubtitle = bilingualSubtitle
        translationViewModel.startTranslation(settings) { jobId, projectId ->
            onNavigate(Routes.processing(jobId, projectId))
        }
    }

    fun onStartClicked() {
        if (voiceMode == VoiceMode.VOICE_SIMILARITY &&
            !translationViewModel.voiceSimilarityConsented
        ) {
            showConsentDialog = true
        } else {
            applyAndStart()
        }
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Xác nhận quyền sử dụng giọng nói") },
            text = {
                Text(
                    "Voice Similarity phân tích đặc điểm giọng nói gốc để tạo " +
                        "giọng dịch tương tự. Chỉ sử dụng tính năng này khi bạn " +
                        "có quyền và sự đồng ý hợp lệ của người nói. " +
                        "Nếu bạn không đồng ý, ứng dụng sẽ KHÔNG gửi " +
                        "voice_similarity=true lên máy chủ."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        translationViewModel.voiceSimilarityConsented = true
                        showConsentDialog = false
                        applyAndStart()
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

    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentCode = targetLanguage,
            onDismiss = { showLanguagePicker = false },
            onSelected = { language ->
                targetLanguage = language.code
                showLanguagePicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Video Translation", style = MaterialTheme.typography.headlineMedium)

        if (uri == null) {
            Text(
                "Chưa chọn video. Quay về trang chủ để chọn video.",
                style = MaterialTheme.typography.bodyMedium,
                color = SilverText
            )
            PrimaryButton(text = "Về trang chủ", onClick = onBack)
            return@Column
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(uri)
                        setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        Text(
            text = translationViewModel.selectedVideoName ?: "Video đã chọn",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Thời lượng: ${formatDuration(metadata?.durationMs ?: 0L)} • " +
                "Độ phân giải: ${formatResolution(metadata?.width ?: 0, metadata?.height ?: 0)} • " +
                "Dung lượng: ${formatFileSize(metadata?.sizeBytes ?: 0L)}",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )

        Text("Ngôn ngữ gốc: Auto Detect (máy chủ tự nhận diện)")
        OutlinedButton(
            onClick = { showLanguagePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ngôn ngữ dịch: ${languageName(targetLanguage)} — chạm để tìm kiếm")
        }

        Text("Voice Mode", style = MaterialTheme.typography.titleMedium)
        VoiceMode.values().forEach { mode ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = voiceMode == mode,
                    onClick = { voiceMode = mode }
                )
                Text(mode.labelVi)
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

        ToggleRow("Subtitle", subtitleEnabled) { subtitleEnabled = it }
        ToggleRow("Background Music", backgroundMusic) { backgroundMusic = it }
        ToggleRow("Keep Original Audio", keepOriginalAudio) { keepOriginalAudio = it }
        ToggleRow("Bilingual Subtitle", bilingualSubtitle) { bilingualSubtitle = it }

        Text(
            "Chế độ: ${settings.processingMode.labelVi} • Backend: ${settings.backendUrl}",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )

        if (state is TranslationUiState.Error) {
            Text(
                (state as TranslationUiState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }

        PrimaryButton(
            text = "START AUTO TRANSLATION",
            onClick = { onStartClicked() },
            enabled = state !is TranslationUiState.Uploading
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Quay lại")
        }
    }
}

@Composable
private fun LanguagePickerDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onSelected: (Language) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) SUPPORTED_LANGUAGES
        else SUPPORTED_LANGUAGES.filter {
            it.nameVi.contains(query, ignoreCase = true) ||
                it.nameNative.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn ngôn ngữ đích") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Tìm kiếm ngôn ngữ") },
                    modifier = Modifier.fillMaxWidth()
                )
                filtered.forEach { language ->
                    Button(
                        onClick = { onSelected(language) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${language.nameVi} (${language.nameNative})" +
                                if (language.code == currentCode) " ✓" else ""
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    Text("Không tìm thấy ngôn ngữ phù hợp.")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}
