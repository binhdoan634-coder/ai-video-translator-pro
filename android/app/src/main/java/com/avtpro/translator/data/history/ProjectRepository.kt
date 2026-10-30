package com.avtpro.translator.data.history

import android.content.Context
import com.avtpro.translator.data.model.Project
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local project history stored as a JSON file in the app's private files dir.
 * This satisfies the spec's "Room OR DataStore + JSON file" option without
 * adding a Room/annotation-processor dependency to the build.
 */
class ProjectRepository(private val context: Context) {

    private val file: File
        get() = File(context.filesDir, "projects.json")

    fun loadProjects(): List<Project> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                Project(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    thumbnailPath = o.optString("thumbnailPath").takeIf { it.isNotBlank() },
                    sourceLanguage = o.optString("sourceLanguage").takeIf { it.isNotBlank() },
                    targetLanguage = o.optString("targetLanguage"),
                    durationMs = o.optLong("durationMs"),
                    processedAtMs = o.optLong("processedAtMs", System.currentTimeMillis()),
                    status = o.optString("status", "completed"),
                    resultVideoPath = o.optString("resultVideoPath").takeIf { it.isNotBlank() },
                    subtitlePath = o.optString("subtitlePath").takeIf { it.isNotBlank() }
                )
            }.sortedByDescending { it.processedAtMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveProject(project: Project) {
        val current = loadProjects().filter { it.id != project.id }
        val updated = listOf(project) + current
        writeAll(updated)
    }

    fun updateProject(project: Project) = saveProject(project)

    fun deleteProject(projectId: String) {
        writeAll(loadProjects().filter { it.id != projectId })
    }

    fun getProject(projectId: String): Project? =
        loadProjects().firstOrNull { it.id == projectId }

    private fun writeAll(projects: List<Project>) {
        val array = JSONArray()
        projects.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("thumbnailPath", p.thumbnailPath ?: "")
            o.put("sourceLanguage", p.sourceLanguage ?: "")
            o.put("targetLanguage", p.targetLanguage)
            o.put("durationMs", p.durationMs)
            o.put("processedAtMs", p.processedAtMs)
            o.put("status", p.status)
            o.put("resultVideoPath", p.resultVideoPath ?: "")
            o.put("subtitlePath", p.subtitlePath ?: "")
            array.put(o)
        }
        file.writeText(array.toString())
    }
}
