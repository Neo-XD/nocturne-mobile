package com.nocturne.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocturne.music.core.model.HomePage
import com.nocturne.music.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val homePage: HomePage) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedChipParam = MutableStateFlow<String?>(null)
    val selectedChipParam: StateFlow<String?> = _selectedChipParam.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome(params: String? = null) {
        _selectedChipParam.value = params
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            musicRepository.getHome(params)
                .onSuccess { homePage ->
                    _uiState.value = HomeUiState.Success(homePage)
                }
                .onFailure { error ->
                    _uiState.value = HomeUiState.Error(error.localizedMessage ?: "Failed to load home feed")
                }
        }
    }
}
