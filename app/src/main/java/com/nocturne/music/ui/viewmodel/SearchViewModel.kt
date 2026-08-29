package com.nocturne.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.music.core.model.SearchResults
import com.nocturne.music.data.repository.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    data object Idle : SearchUiState()
    data object Loading : SearchUiState()
    data class Success(val results: SearchResults) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce typing
            _uiState.value = SearchUiState.Loading
            musicRepository.search(newQuery)
                .onSuccess { results ->
                    _uiState.value = SearchUiState.Success(results)
                }
                .onFailure { error ->
                    _uiState.value = SearchUiState.Error(error.localizedMessage ?: "Search failed")
                }
        }
    }
}
