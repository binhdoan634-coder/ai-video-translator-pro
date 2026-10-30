package com.avtpro.translator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avtpro.translator.data.history.ProjectRepository
import com.avtpro.translator.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _projects.value = withContext(Dispatchers.IO) { repository.loadProjects() }
        }
    }

    fun getProject(projectId: String): Project? = repository.getProject(projectId)

    fun saveProject(project: Project) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.saveProject(project) }
            refresh()
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteProject(project.id)
                project.resultVideoPath?.let { File(it).delete() }
                project.subtitlePath?.let { File(it).delete() }
                project.thumbnailPath?.let { File(it).delete() }
            }
            refresh()
        }
    }
}
