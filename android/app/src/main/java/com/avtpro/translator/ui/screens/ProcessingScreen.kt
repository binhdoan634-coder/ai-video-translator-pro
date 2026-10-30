package com.avtpro.translator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avtpro.translator.data.model.PROCESSING_STEPS
import com.avtpro.translator.data.model.stepIndexFor
import com.avtpro.translator.data.settings.AppSettings
import com.avtpro.translator.navigation.Routes
import com.avtpro.translator.ui.theme.NeonCyan
import com.avtpro.translator.ui.theme.SilverText
import com.avtpro.translator.viewmodel.HistoryViewModel
import com.avtpro.translator.viewmodel.TranslationUiState
import com.avtpro.translator.viewmodel.TranslationViewModel

@Composable
fun ProcessingScreen(
    jobId: String,
    projectId: String,
    settings: AppSettings,
    translationViewModel: TranslationViewModel,
    historyViewModel: HistoryViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by translationViewModel.state.collectAsState()

    // Resume polling only if this screen was opened without an active flow
    // (e.g. after process recreation). When the Translation screen already
    // started polling, state is Uploading/Polling, so this does nothing.
    LaunchedEffect(jobId) {
        if (state is TranslationUiState.Idle) {
            translationViewModel.pollExistingJob(settings, jobId, projectId)
        }
    }

    // When the server reports completed, persist the project and go to Result.
    // The Completed state already carries the downloaded project.
    LaunchedEffect(state) {
        val completed = state as? TranslationUiState.Completed
        if (completed != null) {
            historyViewModel.saveProject(completed.project)
            onNavigate(Routes.result(completed.project.id))
        }
    }

    val job = (state as? TranslationUiState.Polling)?.job
    // Progress comes ONLY from the server. Before the first poll returns,
    // show 0% with "Đang kết nối máy chủ…" — never an invented value.
    val progress = job?.progress?.coerceIn(0, 100) ?: 0
    val currentStepIndex: Int? = job?.let {
        it.stepIndex?.takeIf { index -> index in PROCESSING_STEPS.indices }
            ?: stepIndexFor(it.currentStep)
    }
    val currentStepText = job?.currentStep
        ?: if (state is TranslationUiState.Uploading) "Upload / Import Video"
        else "Đang kết nối máy chủ…"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Đang xử lý", style = MaterialTheme.typography.headlineMedium)
        Text("Job: $jobId", style = MaterialTheme.typography.bodyMedium, color = SilverText)
        Text(
            "Đang chạy: $currentStepText",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "$progress%",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonCyan
        )
        LinearProgressIndicator(
            progress = progress / 100f,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        PROCESSING_STEPS.forEachIndexed { index, step ->
            val marker = when {
                currentStepIndex != null && index < currentStepIndex -> "✓"
                currentStepIndex == index -> "▶"
                else -> "○"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(marker, color = NeonCyan)
                Text(
                    text = "  $step",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (currentStepIndex == index) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        SilverText
                    }
                )
            }
        }

        if (state is TranslationUiState.Error) {
            Text(
                (state as TranslationUiState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedButton(
            onClick = {
                translationViewModel.stopPolling()
                onBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dừng polling / Hủy")
        }
        Text(
            "Lưu ý: dừng polling chỉ dừng việc theo dõi trên app, không tự " +
                "động hủy job trên máy chủ trừ khi backend hỗ trợ. " +
                "Tiến độ trên không phải tiến độ giả lập.",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )
    }
}
