package com.avtpro.translator.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import okhttp3.ResponseBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

/** Copies a content:// URI into the app cache dir so it can be uploaded. */
fun copyUriToCache(context: Context, uri: Uri, fileName: String): File {
    val out = File(context.cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(out).use { output -> input.copyTo(output) }
    }
    return out
}

/** Streams a Retrofit ResponseBody to a file. Returns the file. */
fun saveResponseBodyToFile(body: ResponseBody, outFile: File): File {
    outFile.parentFile?.mkdirs()
    body.byteStream().use { input ->
        FileOutputStream(outFile).use { output -> input.copyTo(output) }
    }
    return outFile
}

/** Copies a finished video into the public Downloads collection (MediaStore). */
fun saveVideoToDownloads(context: Context, source: File, displayName: String): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "Download/AI Video Translator Pro")
    }
    val uri = context.contentResolver.insert(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
    ) ?: return null
    context.contentResolver.openOutputStream(uri)?.use { output ->
        FileInputStream(source).use { input -> input.copyTo(output) }
    }
    return uri
}

fun shareFile(context: Context, file: File, mimeType: String, title: String) {
    val uri: Uri = FileProvider.getUriForFile(
        context, "com.avtpro.translator.fileprovider", file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

fun openFileStream(context: Context, uri: Uri): InputStream? =
    context.contentResolver.openInputStream(uri)
