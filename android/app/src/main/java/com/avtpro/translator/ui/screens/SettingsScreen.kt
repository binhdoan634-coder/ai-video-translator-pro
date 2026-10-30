package com.avtpro.translator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avtpro.translator.data.api.RetrofitClient
import com.avtpro.translator.data.model.ProcessingMode
import com.avtpro.translator.data.model.SUPPORTED_LANGUAGES
import com.avtpro.translator.data.model.VoiceGender
import com.avtpro.translator.data.model.VoiceMode
import com.avtpro.translator.data.model.languageName
import com.avtpro.translator.ui.components.PrimaryButton
import com.avtpro.translator.ui.components.ToggleRow
import com.avtpro.translator.ui.theme.SilverText
import com.avtpro.translator.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    var backendUrl by remember(settings.backendUrl) {
        mutableStateOf(settings.backendUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Text("Ngôn ngữ ứng dụng", style = MaterialTheme.typography.titleMedium)
        Text("Hiện tại: ${languageName(settings.appLanguage)} (giao diện mặc định tiếng Việt)")
        Text(
            "Lưu ý: bản này hiển thị giao diện tiếng Việt. Chọn ngôn ngữ khác " +
                "được lưu lại và sẽ áp dụng khi có bản dịch giao diện tương ứng.",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )
        SUPPORTED_LANGUAGES.take(11).forEach { language ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.appLanguage == language.code,
                    onClick = {
                        settingsViewModel.update { it.copy(appLanguage = language.code) }
                    }
                )
                Text("${language.nameVi} (${language.nameNative})")
            }
        }

        Text("Ngôn ngữ dịch mặc định", style = MaterialTheme.typography.titleMedium)
        Text("Hiện tại: ${languageName(settings.defaultTargetLanguage)}")
        SUPPORTED_LANGUAGES.take(11).forEach { language ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.defaultTargetLanguage == language.code,
                    onClick = {
                        settingsViewModel.update {
                            it.copy(defaultTargetLanguage = language.code)
                        }
                    }
                )
                Text(language.nameVi)
            }
        }

        Text("Giọng đọc mặc định", style = MaterialTheme.typography.titleMedium)
        VoiceGender.values().forEach { gender ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.voiceGender == gender,
                    onClick = {
                        settingsViewModel.update { it.copy(voiceGender = gender) }
                    }
                )
                Text(gender.labelVi)
            }
        }
        VoiceMode.values().forEach { mode ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.defaultVoiceMode == mode,
                    onClick = {
                        settingsViewModel.update { it.copy(defaultVoiceMode = mode) }
                    }
                )
                Text(mode.labelVi)
            }
        }

        Text("Chế độ xử lý", style = MaterialTheme.typography.titleMedium)
        ProcessingMode.values().forEach { mode ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.processingMode == mode,
                    onClick = {
                        settingsViewModel.update { it.copy(processingMode = mode) }
                    }
                )
                Text(mode.labelVi)
            }
        }
        Text(
            "Auto: thử online trước, nếu mất mạng thì báo rõ chưa có model " +
                "offline. Offline trong bản này KHÔNG giả lập xử lý.",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )

        Text("Chất lượng xuất video", style = MaterialTheme.typography.titleMedium)
        listOf("Giữ nguyên", "720p", "1080p", "2K", "4K").forEach { quality ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.exportQuality == quality,
                    onClick = {
                        settingsViewModel.update { it.copy(exportQuality = quality) }
                    }
                )
                Text(quality)
            }
        }

        Text("Chất lượng âm thanh", style = MaterialTheme.typography.titleMedium)
        listOf("Thấp", "Trung bình", "Cao").forEach { quality ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.audioQuality == quality,
                    onClick = {
                        settingsViewModel.update { it.copy(audioQuality = quality) }
                    }
                )
                Text(quality)
            }
        }

        ToggleRow("Phụ đề mặc định", settings.defaultSubtitle) {
            settingsViewModel.update { s -> s.copy(defaultSubtitle = it) }
        }
        ToggleRow("Tự động lưu video", settings.autoSaveVideo) {
            settingsViewModel.update { s -> s.copy(autoSaveVideo = it) }
        }
        ToggleRow("Tự động xóa file tạm", settings.autoDeleteTemp) {
            settingsViewModel.update { s -> s.copy(autoDeleteTemp = it) }
        }

        Text("Địa chỉ backend", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = backendUrl,
            onValueChange = { backendUrl = it },
            label = { Text("Backend URL") },
            placeholder = { Text(RetrofitClient.DEFAULT_BASE_URL) },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Mặc định cho emulator: ${RetrofitClient.DEFAULT_BASE_URL}. " +
                "Trên điện thoại thật, đổi thành IP LAN của máy chạy backend, " +
                "ví dụ http://192.168.1.10:8000.",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )
        PrimaryButton(
            text = "Lưu địa chỉ backend",
            onClick = {
                settingsViewModel.update { it.copy(backendUrl = backendUrl.trim()) }
            }
        )

        Text("Quản lý model offline", style = MaterialTheme.typography.titleMedium)
        Text(
            "Chưa có model offline nào được đóng gói trong bản này. " +
                "Không có nút tải giả: khi backend cung cấp gói model, mục này " +
                "sẽ liệt kê model thật đã tải về.",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )

        Text("Quyền riêng tư", style = MaterialTheme.typography.titleMedium)
        Text(
            "Không nhúng API key vào app. Video và giọng nói không được dùng " +
                "để huấn luyện AI nếu không có sự đồng ý. Xóa dự án trong " +
                "History trong bản này chỉ xóa file local; endpoint " +
                "DELETE /api/projects/{id} đã được định nghĩa trong ApiService " +
                "để nối với backend khi backend sẵn sàng (xem README).",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )

        PrimaryButton(text = "Quay lại", onClick = onBack)
    }
}
