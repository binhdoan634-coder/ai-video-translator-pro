package com.avtpro.translator.util

import com.avtpro.translator.data.model.SubtitleSegment
import java.io.File

fun formatSrtTime(ms: Long): String {
    val hours = ms / 3_600_000
    val minutes = (ms % 3_600_000) / 60_000
    val seconds = (ms % 60_000) / 1000
    val millis = ms % 1000
    return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
}

/** Parses "HH:MM:SS,mmm" (also accepts '.' instead of ','). Returns null on bad input. */
fun parseSrtTime(text: String): Long? {
    val cleaned = text.trim().replace('.', ',')
    val parts = cleaned.split(":", ",")
    if (parts.size != 4) return null
    val h = parts[0].toLongOrNull() ?: return null
    val m = parts[1].toLongOrNull() ?: return null
    val s = parts[2].toLongOrNull() ?: return null
    val ms = parts[3].toLongOrNull() ?: return null
    return h * 3_600_000 + m * 60_000 + s * 1000 + ms
}

fun segmentsToSrt(segments: List<SubtitleSegment>): String {
    val builder = StringBuilder()
    segments.sortedBy { it.startMs }.forEachIndexed { index, seg ->
        builder.append(index + 1).append("\n")
        builder.append(formatSrtTime(seg.startMs))
            .append(" --> ")
            .append(formatSrtTime(seg.endMs)).append("\n")
        builder.append(seg.text.trim()).append("\n\n")
    }
    return builder.toString()
}

fun parseSrt(content: String): List<SubtitleSegment> {
    val segments = mutableListOf<SubtitleSegment>()
    val blocks = content.trim().split(Regex("\n\\s*\n"))
    for (block in blocks) {
        val lines = block.lines().filter { it.isNotBlank() }
        if (lines.size < 2) continue
        // First line may be the index; the time line contains "-->".
        val timeLine = lines.firstOrNull { "-->" in it } ?: continue
        val text = lines.dropWhile { "-->" !in it }.drop(1).joinToString("\n")
        val times = timeLine.split("-->").map { it.trim() }
        if (times.size != 2) continue
        val start = parseSrtTime(times[0]) ?: continue
        val end = parseSrtTime(times[1]) ?: continue
        if (text.isNotBlank()) segments.add(SubtitleSegment(start, end, text))
    }
    return segments.sortedBy { it.startMs }
}

fun loadSegmentsFromFile(file: File): List<SubtitleSegment> {
    if (!file.exists()) return emptyList()
    return try { parseSrt(file.readText()) } catch (_: Exception) { emptyList() }
}
