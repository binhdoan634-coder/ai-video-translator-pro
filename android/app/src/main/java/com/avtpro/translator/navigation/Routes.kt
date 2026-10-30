package com.avtpro.translator.navigation

object Routes {
    const val HOME = "home"
    const val TRANSLATION = "translation"
    const val AUDIO_DUBBING = "audio_dubbing"
    const val SUBTITLE_STUDIO = "subtitle_studio?projectId={projectId}"
    const val PROCESSING = "processing/{jobId}/{projectId}"
    const val RESULT = "result/{projectId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun subtitleStudio(projectId: String? = null): String =
        if (projectId.isNullOrBlank()) "subtitle_studio" else "subtitle_studio?projectId=$projectId"

    fun processing(jobId: String, projectId: String): String =
        "processing/$jobId/$projectId"

    fun result(projectId: String): String = "result/$projectId"
}
