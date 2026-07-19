package com.innocent254.wuwa.companion.core.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.innocent254.wuwa.companion.ui.model.CompanionUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CatalogRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(repository.emptyState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = withContext(Dispatchers.IO) {
                runCatching(repository::load).getOrElse { error ->
                    repository.emptyState(error = error.message ?: error.javaClass.simpleName)
                }
            }
        }
    }
}
