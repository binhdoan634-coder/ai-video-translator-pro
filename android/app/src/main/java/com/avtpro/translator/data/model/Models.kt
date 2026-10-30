package com.avtpro.translator.data.model

import com.google.gson.annotations.SerializedName

// ---------------------------------------------------------------------------
// Languages, voices, modes — UI-facing domain models (not backend DTOs)
// ---------------------------------------------------------------------------

data class Language(val code: String, val nameVi: String, val nameNative: String)

/** The 11 required languages first, then additional languages. */
val SUPPORTED_LANGUAGES = listOf(
    Language("vi", "Tiếng Việt", "Tiếng Việt"),
    Language("en", "Tiếng Anh", "English"),
    Language("zh", "Tiếng Trung", "中文"),
    Language("ja", "Tiếng Nhật", "日本語"),
    Language("ko", "Tiếng Hàn", "한국어"),
    Language("th", "Tiếng Thái", "ไทย"),
    Language("fr", "Tiếng Pháp", "Français"),
    Language("de", "Tiếng Đức", "Deutsch"),
    Language("es", "Tiếng Tây Ban Nha", "Español"),
    Language("pt", "Tiếng Bồ Đào Nha", "Português"),
    Language("ru", "Tiếng Nga", "Русский"),
    Language("it", "Tiếng Ý", "Italiano"),
    Language("nl", "Tiếng Hà Lan", "Nederlands"),
    Language("id", "Tiếng Indonesia", "Bahasa Indonesia"),
    Language("ms", "Tiếng Mã Lai", "Bahasa Melayu"),
    Language("ar", "Tiếng Ả Rập", "العربية"),
    Language("hi", "Tiếng Hindi", "हिन्दी"),
    Language("tr", "Tiếng Thổ Nhĩ Kỳ", "Türkçe"),
    Language("pl", "Tiếng Ba Lan", "Polski"),
    Language("uk", "Tiếng Ukraina", "Українська")
)

fun languageName(code: String): String =
    SUPPORTED_LANGUAGES.firstOrNull { it.code == code }?.nameVi ?: code

enum class VoiceMode(val apiValue: String, val labelVi: String) {
    NATURAL("natural", "Natural Voice"),
    // Backend expects "similarity" (not "voice_similarity") for voice_mode.
    VOICE_SIMILARITY("similarity", "Voice Similarity")
}

enum class ProcessingMode(val labelVi: String) {
    AUTO("Auto"),
    ONLINE("Online"),
    OFFLINE("Offline")
}

enum class VoiceGender(val labelVi: String) {
    MALE("Giọng nam"),
    FEMALE("Giọng nữ")
}

// ---------------------------------------------------------------------------
// The 9 real processing steps. The app NEVER invents a step or a percentage:
// both come from GET /api/jobs/{id} (see JobStatusResponse below).
// ---------------------------------------------------------------------------

val PROCESSING_STEPS = listOf(
    "Upload / Import Video",
    "Extract Audio",
    "Speech Recognition",
    "Speaker Detection",
    "Translate Text",
    "Generate AI Voice",
    "Synchronize Audio",
    "Generate Subtitle",
    "Render Final Video"
)

/** Maps a backend current_step string (English key or Vietnamese text) to a step index. */
fun stepIndexFor(currentStep: String?): Int? {
    if (currentStep.isNullOrBlank()) return null
    val s = currentStep.lowercase()
    return when {
        "upload" in s || "import" in s || "nhập" in s -> 0
        "extract" in s || "tách" in s -> 1
        "speech" in s || "recognition" in s || "nhận diện" in s -> 2
        "speaker" in s || "người nói" in s -> 3
        "translat" in s || "dịch" in s -> 4
        "voice" in s || "giọng" in s -> 5
        "synchron" in s || "đồng bộ" in s -> 6
        "subtitle" in s || "phụ đề" in s -> 7
        "render" in s || "xuất" in s || "ghép" in s -> 8
        else -> null
    }
}

// ---------------------------------------------------------------------------
// Backend DTOs — field names match the backend contract in the task spec.
// The backend team must return exactly these names; see README_BACKEND_CONTRACT.
// ---------------------------------------------------------------------------

data class UploadResponse(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("filename") val filename: String? = null
)

// Field names must match backend/app/main.py TranslateRequest exactly.
// The backend ignores unknown fields, so the old names (subtitle_enabled,
// background_music, bilingual_subtitle, voice_similarity) were silently
// dropped — fixed here 2026-10-30 during parent verification.
data class TranslateRequest(
    @SerializedName("project_id") val projectId: String,
    @SerializedName("target_language") val targetLanguage: String,
    @SerializedName("voice_mode") val voiceMode: String,
    @SerializedName("voice_gender") val voiceGender: String? = null,
    @SerializedName("subtitle_on") val subtitleOn: Boolean,
    @SerializedName("bilingual") val bilingual: Boolean,
    @SerializedName("hardcode_subtitle") val hardcodeSubtitle: Boolean,
    @SerializedName("keep_background") val keepBackground: Boolean,
    @SerializedName("keep_original_audio") val keepOriginalAudio: Boolean,
    @SerializedName("voice_similarity_consent") val voiceSimilarityConsent: Boolean,
    @SerializedName("translation_style") val translationStyle: String = "natural"
)

data class TranslateResponse(
    @SerializedName("job_id") val jobId: String
)

data class JobStatusResponse(
    // Backend job JSON uses "id", not "job_id".
    @SerializedName(value = "job_id", alternate = ["id"]) val jobId: String,
    @SerializedName("status") val status: String,
    @SerializedName("current_step") val currentStep: String? = null,
    @SerializedName("step_index") val stepIndex: Int? = null,
    /** Server-reported progress, 0..100. The app displays it verbatim. */
    @SerializedName("progress") val progress: Int? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("source_language") val sourceLanguage: String? = null,
    @SerializedName("target_language") val targetLanguage: String? = null
) {
    val isCompleted: Boolean get() = status.equals("completed", ignoreCase = true)
    val isFailed: Boolean get() = status.equals("failed", ignoreCase = true)
    val isCancelled: Boolean get() = status.equals("cancelled", ignoreCase = true)
}

data class ResultResponse(
    @SerializedName(value = "project_id", alternate = ["id"]) val projectId: String? = null,
    @SerializedName("job_id") val jobId: String? = null,
    /** Backend returns result_download_url, e.g. "/api/files/<id>/video". */
    @SerializedName(value = "video_url", alternate = ["result_download_url"]) val videoUrl: String? = null,
    /** Backend returns subtitle_srt_url, e.g. "/api/files/<id>/srt". */
    @SerializedName(value = "subtitle_url", alternate = ["subtitle_srt_url"]) val subtitleUrl: String? = null,
    @SerializedName("subtitle_srt") val subtitleSrt: String? = null,
    @SerializedName("source_language") val sourceLanguage: String? = null,
    @SerializedName("target_language") val targetLanguage: String? = null
)

data class TranscribeResponse(
    @SerializedName("text") val text: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("job_id") val jobId: String? = null
)

data class DubResponse(
    @SerializedName("job_id") val jobId: String? = null,
    // Backend /api/audio/dub returns dub_id + download_url (no job_id).
    @SerializedName("dub_id") val dubId: String? = null,
    @SerializedName(value = "audio_url", alternate = ["download_url"]) val audioUrl: String? = null,
    @SerializedName("status") val status: String? = null
)

data class SubtitleGenerateRequest(
    @SerializedName("project_id") val projectId: String? = null,
    @SerializedName("target_language") val targetLanguage: String,
    @SerializedName("segments") val segments: List<SubtitleSegmentDto>? = null
)

data class SubtitleSegmentDto(
    @SerializedName("start_ms") val startMs: Long,
    @SerializedName("end_ms") val endMs: Long,
    @SerializedName("text") val text: String
)

// ---------------------------------------------------------------------------
// Local history project — stored as JSON in the app's files dir (see
// ProjectRepository). No Room dependency is required for this storage choice.
// ---------------------------------------------------------------------------

data class Project(
    val id: String,
    val name: String,
    val thumbnailPath: String? = null,
    val sourceLanguage: String? = null,
    val targetLanguage: String,
    val durationMs: Long = 0L,
    val processedAtMs: Long = System.currentTimeMillis(),
    val status: String = "completed",
    val resultVideoPath: String? = null,
    val subtitlePath: String? = null
)

data class SubtitleSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)
