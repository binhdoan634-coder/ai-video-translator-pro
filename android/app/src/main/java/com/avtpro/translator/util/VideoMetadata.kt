package com.avtpro.translator.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val displayName: String?
)

/** Real metadata via MediaMetadataRetriever + ContentResolver. No estimates. */
fun getVideoMetadata(context: Context, uri: Uri): VideoMetadata {
    var durationMs = 0L
    var width = 0
    var height = 0
    var sizeBytes = 0L
    var displayName: String? = null

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                sizeBytes = cursor.getLong(sizeIndex)
            }
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                displayName = cursor.getString(nameIndex)
            }
        }
    }
    // Fallback for file:// URIs where the resolver may not report a size.
    if (sizeBytes == 0L && uri.scheme == "file") {
        uri.path?.let { sizeBytes = File(it).length() }
    }

    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        durationMs = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLongOrNull() ?: 0L
        width = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
        )?.toIntOrNull() ?: 0
        height = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
        )?.toIntOrNull() ?: 0
    } catch (_: Exception) {
        // Keep whatever was already resolved; the UI shows "Không xác định"
        // for missing fields instead of inventing values.
    } finally {
        try { retriever.release() } catch (_: Exception) { }
    }
    return VideoMetadata(durationMs, width, height, sizeBytes, displayName)
}

/** Extracts the first frame as a JPEG thumbnail for the history list. */
fun extractThumbnail(context: Context, uri: Uri, projectId: String): String? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val bitmap: Bitmap = retriever.getFrameAtTime(0)
            ?: retriever.getFrameAtTime(1_000_000) ?: return null
        val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val out = File(dir, "$projectId.jpg")
        FileOutputStream(out).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        }
        out.absolutePath
    } catch (_: Exception) {
        null
    } finally {
        try { retriever.release() } catch (_: Exception) { }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "Không xác định"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Không xác định"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
}

fun formatResolution(width: Int, height: Int): String =
    if (width > 0 && height > 0) "${width}x$height" else "Không xác định"
