package com.avtpro.translator.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avtpro.translator.data.api.RetrofitClient
import com.avtpro.translator.data.model.JobStatusResponse
import com.avtpro.translator.data.model.ProcessingMode
import com.avtpro.translator.data.model.Project
import com.avtpro.translator.data.model.TranslateRequest
import com.avtpro.translator.data.model.VoiceGender
import com.avtpro.translator.data.model.VoiceMode
import com.avtpro.translator.data.settings.AppSettings
import com.avtpro.translator.util.VideoMetadata
import com.avtpro.translator.util.copyUriToCache
import com.avtpro.translator.util.extractThumbnail
import com.avtpro.translator.util.saveResponseBodyToFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID

sealed interface TranslationUiState {
    data object Idle : TranslationUiState
    data object Uploading : TranslationUiState
    data class Polling(val job: JobStatusResponse) : TranslationUiState
    data class Completed(val project: Project) : TranslationUiState
    data class Error(val message: String) : TranslationUiState
}

/**
 * Owns the whole AUTO translation flow:
 *   upload (multipart) -> translate -> poll GET /api/jobs/{id} every 2s
 *   -> on completed, GET /api/video/result/{id} and download the file.
 *
 * Progress and current_step are ALWAYS taken from the server response.
 * Nothing here increments, estimates or fabricates a percentage.
 * Stopping polling cancels the coroutine; it does not claim completion.
 */
class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    var selectedVideoUri: Uri? = null
    var selectedVideoMetadata: VideoMetadata? = null
    var selectedVideoName: String? = null

    var targetLanguage: String = "vi"
    var voiceMode: VoiceMode = VoiceMode.NATURAL
    var voiceGender: VoiceGender = VoiceGender.FEMALE
    var subtitleEnabled: Boolean = true
    // Default OFF: backend keep_background=true requires demucs, otherwise
    // the job fails honestly instead of silently dropping the music.
    // User can opt in on the Translation screen when demucs is available.
    var backgroundMusic: Boolean = false
    var keepOriginalAudio: Boolean = false
    var bilingualSubtitle: Boolean = false

    /** Set only by the consent dialog. Never defaults to true. */
    var voiceSimilarityConsented: Boolean = false

    var currentJobId: String? = null
        private set

    private val _state = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val state: StateFlow<TranslationUiState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    fun resetForNewVideo() {
        pollingJob?.cancel()
        pollingJob = null
        currentJobId = null
        voiceSimilarityConsented = false
        _state.value = TranslationUiState.Idle
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        if (_state.value !is TranslationUiState.Completed) {
            _state.value = TranslationUiState.Idle
        }
    }

    fun startTranslation(settings: AppSettings, onJobStarted: (String, String) -> Unit) {
        val uri = selectedVideoUri ?: run {
            _state.value = TranslationUiState.Error("Chưa chọn video.")
            return
        }
        // Voice Similarity gate: no consent, no voice_similarity=true, ever.
        val useVoiceSimilarity = voiceMode == VoiceMode.VOICE_SIMILARITY &&
            voiceSimilarityConsented

        viewModelScope.launch {
            _state.value = TranslationUiState.Uploading
            // Upload + translate are in the try block; polling is deliberately
            // outside it, so cancelling polling never turns into a fake error.
            val started: Pair<String, String>? = try {
                val api = RetrofitClient.create(settings.backendUrl)
                val uploadFile = withContext(Dispatchers.IO) {
                    copyUriToCache(
                        appContext, uri,
                        "upload_${UUID.randomUUID()}.mp4"
                    )
                }
                val requestBody = uploadFile.asRequestBody("video/mp4".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "file", uploadFile.name, requestBody
                )
                val upload = api.uploadVideo(part)
                if (settings.autoDeleteTemp) uploadFile.delete()

                val translateResponse = api.translateVideo(
                    TranslateRequest(
                        projectId = upload.projectId,
                        targetLanguage = targetLanguage,
                        // Backend detects source language itself; no
                        // source_language field in its TranslateRequest.
                        // Similarity without consent falls back to natural
                        // here; the screen dialog is the consent gate.
                        voiceMode = if (useVoiceSimilarity) "similarity" else "natural",
                        voiceGender = voiceGender.name.lowercase(),
                        subtitleOn = subtitleEnabled,
                        bilingual = bilingualSubtitle,
                        hardcodeSubtitle = subtitleEnabled,
                        keepBackground = backgroundMusic,
                        keepOriginalAudio = keepOriginalAudio,
                        voiceSimilarityConsent = useVoiceSimilarity,
                        translationStyle = "natural"
                    )
                )
                currentJobId = translateResponse.jobId
                onJobStarted(translateResponse.jobId, upload.projectId)
                translateResponse.jobId to upload.projectId
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = TranslationUiState.Error(
                    offlineAwareMessage(settings.processingMode, e)
                )
                null
            }
            if (started != null) {
                pollJob(settings, started.first, started.second)
            }
        }
    }

    fun pollExistingJob(settings: AppSettings, jobId: String, projectId: String) {
        currentJobId = jobId
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            pollJob(settings, jobId, projectId)
        }
    }

    private suspend fun pollJob(settings: AppSettings, jobId: String, projectId: String) {
        val api = RetrofitClient.create(settings.backendUrl)
        pollingJob = viewModelScope.launch {
            try {
                while (isActive) {
                    val job = api.getJob(jobId)
                    _state.value = TranslationUiState.Polling(job)
                    when {
                        job.isCompleted -> {
                            val project = downloadResult(settings, job, projectId)
                            _state.value = TranslationUiState.Completed(project)
                            return@launch
                        }
                        job.isFailed -> {
                            _state.value = TranslationUiState.Error(
                                job.error ?: "Xử lý thất bại trên máy chủ."
                            )
                            return@launch
                        }
                        job.isCancelled -> {
                            _state.value = TranslationUiState.Idle
                            return@launch
                        }
                    }
                    delay(2000L)
                }
            } catch (e: Exception) {
                if (isActive) {
                    _state.value = TranslationUiState.Error(
                        offlineAwareMessage(settings.processingMode, e)
                    )
                }
            }
        }
        pollingJob?.join()
    }

    private suspend fun downloadResult(
        settings: AppSettings,
        job: JobStatusResponse,
        projectId: String
    ): Project {
        val api = RetrofitClient.create(settings.backendUrl)
        val result = api.getResult(projectId)
        val resultDir = File(appContext.filesDir, "results").apply { mkdirs() }
        val subtitleDir = File(appContext.filesDir, "subtitles").apply { mkdirs() }

        var resultVideoPath: String? = null
        var subtitlePath: String? = null

        val videoUrl = result.videoUrl
        if (!videoUrl.isNullOrBlank()) {
            val outFile = File(resultDir, "$projectId.mp4")
            withContext(Dispatchers.IO) {
                val body = api.downloadFile(
                    RetrofitClient.resolveUrl(settings.backendUrl, videoUrl)
                )
                saveResponseBodyToFile(body, outFile)
            }
            resultVideoPath = outFile.absolutePath
        }

        val subtitleUrl = result.subtitleUrl
        val subtitleSrt = result.subtitleSrt
        when {
            !subtitleSrt.isNullOrBlank() -> {
                val outFile = File(subtitleDir, "$projectId.srt")
                outFile.writeText(subtitleSrt)
                subtitlePath = outFile.absolutePath
            }
            !subtitleUrl.isNullOrBlank() -> {
                val outFile = File(subtitleDir, "$projectId.srt")
                withContext(Dispatchers.IO) {
                    val body = api.downloadFile(
                        RetrofitClient.resolveUrl(settings.backendUrl, subtitleUrl)
                    )
                    saveResponseBodyToFile(body, outFile)
                }
                subtitlePath = outFile.absolutePath
            }
        }

        val thumbnailPath = withContext(Dispatchers.IO) {
            selectedVideoUri?.let { uri ->
                extractThumbnail(appContext, uri, projectId)
            }
        }

        return Project(
            id = projectId,
            name = selectedVideoName ?: "Video $projectId",
            thumbnailPath = thumbnailPath,
            sourceLanguage = result.sourceLanguage ?: job.sourceLanguage,
            targetLanguage = result.targetLanguage ?: job.targetLanguage ?: targetLanguage,
            durationMs = selectedVideoMetadata?.durationMs ?: 0L,
            processedAtMs = System.currentTimeMillis(),
            status = "completed",
            resultVideoPath = resultVideoPath,
            subtitlePath = subtitlePath
        )
    }

    private fun offlineAwareMessage(mode: ProcessingMode, e: Exception): String {
        val detail = e.message ?: e.javaClass.simpleName
        return when (mode) {
            ProcessingMode.OFFLINE ->
                "Chưa có model offline trong bản này, cần kết nối backend. " +
                    "Lỗi: $detail"
            ProcessingMode.AUTO ->
                "Không kết nối được backend, và bản này chưa có model offline " +
                    "để xử lý thay thế. Kiểm tra địa chỉ backend trong Settings. Lỗi: $detail"
            ProcessingMode.ONLINE ->
                "Không kết nối được backend. Kiểm tra địa chỉ backend trong " +
                    "Settings và kết nối mạng. Lỗi: $detail"
        }
    }
}
