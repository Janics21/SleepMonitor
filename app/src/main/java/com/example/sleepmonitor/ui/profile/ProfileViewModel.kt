package com.example.sleepmonitor.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepmonitor.data.local.entities.UserEntity
import com.example.sleepmonitor.data.repository.AuthRepository
import com.example.sleepmonitor.ui.utils.SessionManager
import kotlinx.coroutines.launch

sealed class ProfileState {
    data object Loading : ProfileState()
    data class Ready(val user: UserEntity?) : ProfileState()
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableLiveData<ProfileState>(ProfileState.Loading)
    val state: LiveData<ProfileState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            val user = userId?.let { authRepository.getUserById(it) }
            _state.postValue(ProfileState.Ready(user))
        }
    }
}
