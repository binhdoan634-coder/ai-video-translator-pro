package com.avtpro.translator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avtpro.translator.data.model.SubtitleSegment
import com.avtpro.translator.ui.components.PrimaryButton
import com.avtpro.translator.ui.theme.NavySurface
import com.avtpro.translator.ui.theme.SilverText
import com.avtpro.translator.util.formatSrtTime
import com.avtpro.translator.util.loadSegmentsFromFile
import com.avtpro.translator.util.parseSrtTime
import com.avtpro.translator.util.segmentsToSrt
import com.avtpro.translator.util.shareFile
import com.avtpro.translator.viewmodel.HistoryViewModel
import java.io.File

enum class SubtitlePosition(val labelVi: String) {
    TOP("Trên"),
    CENTER("Giữa"),
    BOTTOM("Dưới")
}

@Composable
fun SubtitleStudioScreen(
    projectId: String?,
    historyViewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val project = remember(projectId) {
        projectId?.takeIf { it.isNotBlank() }?.let { historyViewModel.getProject(it) }
    }
    val initialSegments = remember(project) {
        project?.subtitlePath?.let { File(it) }?.let { loadSegmentsFromFile(it) }
            ?: emptyList()
    }
    var segments by remember { mutableStateOf(initialSegments) }
    var fontSize by remember { mutableStateOf(18f) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var position by remember { mutableStateOf(SubtitlePosition.BOTTOM) }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Subtitle Studio", style = MaterialTheme.typography.headlineMedium)
        if (project == null) {
            Text(
                "Chưa chọn dự án. Vào Video History, chọn một dự án rồi bấm " +
                    "\"Sửa phụ đề\" để mở phụ đề thật của dự án đó.",
                color = SilverText
            )
            PrimaryButton(text = "Quay lại", onClick = onBack)
            return@Column
        }
        Text("Dự án: ${project.name}", style = MaterialTheme.typography.titleMedium)
        if (segments.isEmpty()) {
            Text(
                "Dự án này chưa có phụ đề. Bạn có thể thêm câu mới bên dưới.",
                color = SilverText
            )
        }

        Text("Cỡ chữ: ${fontSize.toInt()}sp")
        Slider(
            value = fontSize,
            onValueChange = { fontSize = it },
            valueRange = 12f..32f
        )
        Text("Màu chữ", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Trắng" to Color.White,
                "Vàng" to Color.Yellow,
                "Xanh neon" to Color(0xFF00E5FF),
                "Đỏ" to Color.Red
            ).forEach { (label, color) ->
                OutlinedButton(onClick = { selectedColor = color }) { Text(label) }
            }
        }
        Text("Vị trí", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubtitlePosition.values().forEach { candidate ->
                OutlinedButton(onClick = { position = candidate }) {
                    Text(candidate.labelVi)
                }
            }
        }
        Text(
            "Đang chọn: cỡ ${fontSize.toInt()}sp, vị trí ${position.labelVi}. " +
                "Các tùy chọn này áp dụng khi xuất phụ đề gắn trực tiếp " +
                "(hardcoded) ở backend; file SRT xuất ra giữ nguyên text + thời gian.",
            style = MaterialTheme.typography.bodyMedium,
            color = SilverText
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(segments, key = { index, seg -> "$index-${seg.startMs}" }) { index, seg ->
                SegmentEditor(
                    segment = seg,
                    fontSize = fontSize,
                    color = selectedColor,
                    onChanged = { updated ->
                        segments = segments.toMutableList().also { it[index] = updated }
                    },
                    onDelete = {
                        segments = segments.toMutableList().also { it.removeAt(index) }
                    }
                )
            }
        }

        PrimaryButton(
            text = "Thêm câu mới",
            onClick = {
                val lastEnd = segments.maxOfOrNull { it.endMs } ?: 0L
                segments = segments + SubtitleSegment(lastEnd, lastEnd + 3000L, "")
            }
        )
        PrimaryButton(
            text = "Xuất file SRT",
            onClick = {
                val outDir = File(context.filesDir, "subtitles").apply { mkdirs() }
                val out = File(outDir, "${project.id}-edited.srt")
                out.writeText(segmentsToSrt(segments))
                // Persist the edited subtitle back to the project record.
                historyViewModel.saveProject(project.copy(subtitlePath = out.absolutePath))
                shareFile(context, out, "text/plain", "Xuất phụ đề SRT")
                status = "Đã xuất SRT và lưu vào dự án."
            }
        )
        if (status.isNotBlank()) Text(status, color = SilverText)
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Quay lại")
        }
    }
}

@Composable
private fun SegmentEditor(
    segment: SubtitleSegment,
    fontSize: Float,
    color: Color,
    onChanged: (SubtitleSegment) -> Unit,
    onDelete: () -> Unit
) {
    var startText by remember(segment) { mutableStateOf(formatSrtTime(segment.startMs)) }
    var endText by remember(segment) { mutableStateOf(formatSrtTime(segment.endMs)) }
    var text by remember(segment) { mutableStateOf(segment.text) }

    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onChanged(segment.copy(text = it))
                },
                label = { Text("Nội dung phụ đề") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize.sp,
                    color = color
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { value ->
                        startText = value
                        parseSrtTime(value)?.let { onChanged(segment.copy(startMs = it)) }
                    },
                    label = { Text("Bắt đầu") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { value ->
                        endText = value
                        parseSrtTime(value)?.let { onChanged(segment.copy(endMs = it)) }
                    },
                    label = { Text("Kết thúc") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onDelete) { Text("Xóa") }
            }
        }
    }
}
