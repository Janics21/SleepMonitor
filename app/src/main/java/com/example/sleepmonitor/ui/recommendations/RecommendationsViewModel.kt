package com.example.sleepmonitor.ui.recommendations

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.sleepmonitor.data.local.entities.RecommendationEntity
import com.example.sleepmonitor.data.repository.RecommendationRepository
import com.example.sleepmonitor.ui.utils.SessionManager
import kotlinx.coroutines.launch

class RecommendationsViewModel(
    private val repository: RecommendationRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val userId: String? = sessionManager.getUserId()

    init {
        userId?.let { uid ->
            viewModelScope.launch {
                repository.syncFromBackend(uid)
            }
        }
    }

    val recommendations: LiveData<List<RecommendationEntity>> =
        userId?.let { repository.getRecommendationsFlow(it).asLiveData() }
            ?: liveData { emit(emptyList()) }

    val showGeneric: LiveData<Boolean> = liveData {
        val uid = userId
        if (uid == null) {
            emit(true)
        } else {
            emit(!repository.hasRecommendations(uid))
        }
    }

    fun getGenericRecommendations(): List<Pair<String, String>> {
        return repository.getGenericRecommendations()
    }
}
