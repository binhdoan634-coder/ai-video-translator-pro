package com.avtpro.translator.ui.screens

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.avtpro.translator.data.model.languageName
import com.avtpro.translator.navigation.Routes
import com.avtpro.translator.ui.components.PrimaryButton
import com.avtpro.translator.ui.theme.NavySurface
import com.avtpro.translator.ui.theme.SilverText
import com.avtpro.translator.util.formatDuration
import com.avtpro.translator.util.saveVideoToDownloads
import com.avtpro.translator.util.shareFile
import com.avtpro.translator.viewmodel.HistoryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultScreen(
    projectId: String,
    historyViewModel: HistoryViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val project = remember(projectId) { historyViewModel.getProject(projectId) }
    var resultVideoView by remember { mutableStateOf<VideoView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Kết quả", style = MaterialTheme.typography.headlineMedium)

        if (project == null) {
            Text("Không tìm thấy dự án này trong lịch sử local.")
            PrimaryButton(text = "Về trang chủ", onClick = onBack)
            return@Column
        }

        val resultFile = project.resultVideoPath?.let { File(it) }
        if (resultFile != null && resultFile.exists()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(resultFile.toUri())
                            setMediaController(
                                MediaController(ctx).also { it.setAnchorView(this) }
                            )
                            resultVideoView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        } else {
            Text(
                "Máy chủ báo hoàn thành nhưng app chưa tải được file video " +
                    "kết quả (xem lịch sử hoặc thử lại).",
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(project.name, style = MaterialTheme.typography.titleMedium)
        Text(
            "Ngôn ngữ: ${project.sourceLanguage?.let { languageName(it) } ?: "Auto Detect"} " +
                "→ ${languageName(project.targetLanguage)} • " +
                "Thời lượng: ${formatDuration(project.durationMs)} • " +
                "Ngày: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(Date(project.processedAtMs)),
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )

        PrimaryButton(
            text = "Play",
            onClick = { resultVideoView?.start() }
        )
        PrimaryButton(
            text = "Download",
            onClick = {
                if (resultFile != null && resultFile.exists()) {
                    saveVideoToDownloads(
                        context, resultFile, "${project.name}.mp4"
                    )
                }
            }
        )
        PrimaryButton(
            text = "Share",
            onClick = {
                if (resultFile != null && resultFile.exists()) {
                    shareFile(context, resultFile, "video/mp4", "Chia sẻ video")
                }
            }
        )
        PrimaryButton(
            text = "Export Subtitle",
            onClick = {
                val subtitleFile = project.subtitlePath?.let { File(it) }
                if (subtitleFile != null && subtitleFile.exists()) {
                    shareFile(context, subtitleFile, "text/plain", "Xuất phụ đề SRT")
                }
            },
            enabled = project.subtitlePath != null
        )
        PrimaryButton(
            text = "Translate Again",
            onClick = { onNavigate(Routes.HOME) }
        )
        OutlinedButton(
            onClick = {
                historyViewModel.deleteProject(project)
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete Project")
        }
    }
}
