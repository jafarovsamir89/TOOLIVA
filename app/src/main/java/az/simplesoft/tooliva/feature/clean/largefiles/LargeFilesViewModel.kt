package az.simplesoft.tooliva.feature.clean.largefiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.LargeMediaFile
import az.simplesoft.tooliva.core.media.MediaStoreLargeFileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LargeFilesUiState(
    val isLoading: Boolean = false,
    val files: List<LargeMediaFile> = emptyList(),
    val errorMessage: String? = null,
)

class LargeFilesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreLargeFileRepository(application)

    private val _uiState = MutableStateFlow(LargeFilesUiState())
    val uiState = _uiState.asStateFlow()

    fun scan() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.scan() }
                .onSuccess { files ->
                    _uiState.value = LargeFilesUiState(files = files)
                }
                .onFailure { error ->
                    _uiState.value = LargeFilesUiState(
                        errorMessage = error.message ?: "Unable to scan media files.",
                    )
                }
        }
    }
}
