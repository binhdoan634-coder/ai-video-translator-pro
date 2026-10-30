package com.avtpro.translator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avtpro.translator.data.model.Project
import com.avtpro.translator.data.model.languageName
import com.avtpro.translator.navigation.Routes
import com.avtpro.translator.ui.components.PrimaryButton
import com.avtpro.translator.ui.theme.NavySurface
import com.avtpro.translator.ui.theme.SilverText
import com.avtpro.translator.util.formatDuration
import com.avtpro.translator.viewmodel.HistoryViewModel
import com.avtpro.translator.viewmodel.TranslationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    translationViewModel: TranslationViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val projects by historyViewModel.projects.collectAsState()

    LaunchedEffect(Unit) { historyViewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Video History", style = MaterialTheme.typography.headlineMedium)
        if (projects.isEmpty()) {
            Text(
                "Chưa có dự án nào. Hoàn thành một bản dịch để dự án xuất hiện ở đây.",
                color = SilverText
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projects, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    onOpen = { onNavigate(Routes.result(project.id)) },
                    onTranslateAgain = {
                        // Re-translate: go to Home so the user picks the
                        // source video again (the app does not keep the
                        // original upload, only the finished result).
                        translationViewModel.resetForNewVideo()
                        onBack()
                    },
                    onEditSubtitles = {
                        onNavigate(Routes.subtitleStudio(project.id))
                    },
                    onDelete = { historyViewModel.deleteProject(project) }
                )
            }
        }
        PrimaryButton(text = "Về trang chủ", onClick = onBack)
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onOpen: () -> Unit,
    onTranslateAgain: () -> Unit,
    onEditSubtitles: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(project.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${project.sourceLanguage?.let { languageName(it) } ?: "Auto"} " +
                    "→ ${languageName(project.targetLanguage)} • " +
                    formatDuration(project.durationMs) + " • " +
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(Date(project.processedAtMs)),
                style = MaterialTheme.typography.bodyMedium,
                color = SilverText
            )
            Text("Trạng thái: ${project.status}", color = SilverText)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) { Text("Xem lại") }
                OutlinedButton(onClick = onEditSubtitles) { Text("Sửa phụ đề") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTranslateAgain) { Text("Dịch lại") }
                OutlinedButton(onClick = onDelete) { Text("Xóa") }
            }
        }
    }
}
