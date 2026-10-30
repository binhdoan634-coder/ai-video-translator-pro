package com.avtpro.translator.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.avtpro.translator.data.settings.AppSettings
import com.avtpro.translator.navigation.Routes
import com.avtpro.translator.ui.components.BrandHeader
import com.avtpro.translator.ui.components.MenuCard
import com.avtpro.translator.ui.components.PrimaryButton
import com.avtpro.translator.util.getVideoMetadata
import com.avtpro.translator.viewmodel.TranslationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    settings: AppSettings,
    translationViewModel: TranslationViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val metadata = withContext(Dispatchers.IO) {
                    getVideoMetadata(context, uri)
                }
                translationViewModel.resetForNewVideo()
                translationViewModel.selectedVideoUri = uri
                translationViewModel.selectedVideoMetadata = metadata
                translationViewModel.selectedVideoName = metadata.displayName ?: "Video đã chọn"
                translationViewModel.targetLanguage = settings.defaultTargetLanguage
                translationViewModel.voiceMode = settings.defaultVoiceMode
                translationViewModel.voiceGender = settings.voiceGender
                translationViewModel.subtitleEnabled = settings.defaultSubtitle
                onNavigate(Routes.TRANSLATION)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrandHeader()
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = "Chọn video",
            onClick = { pickVideo.launch("video/*") }
        )
        MenuCard(
            title = "Video Translation",
            subtitle = "Dịch toàn bộ video, lồng tiếng AI và tạo phụ đề tự động.",
            onClick = { pickVideo.launch("video/*") }
        )
        MenuCard(
            title = "Audio Dubbing",
            subtitle = "Dịch file âm thanh và tạo giọng đọc AI, xuất MP3/WAV.",
            onClick = { onNavigate(Routes.AUDIO_DUBBING) }
        )
        MenuCard(
            title = "Auto Subtitle",
            subtitle = "Xem và chỉnh sửa phụ đề của dự án đã xử lý.",
            onClick = { onNavigate(Routes.subtitleStudio()) }
        )
        MenuCard(
            title = "Voice Studio",
            subtitle = "Tạo giọng đọc AI từ file âm thanh (dùng chung Audio Dubbing).",
            onClick = { onNavigate(Routes.AUDIO_DUBBING) }
        )
        MenuCard(
            title = "Video History",
            subtitle = "Xem lại, dịch lại ngôn ngữ khác hoặc xóa dự án.",
            onClick = { onNavigate(Routes.HISTORY) }
        )
        MenuCard(
            title = "Settings",
            subtitle = "Ngôn ngữ, giọng đọc, chế độ Online/Offline/Auto, địa chỉ backend.",
            onClick = { onNavigate(Routes.SETTINGS) }
        )
    }
}
