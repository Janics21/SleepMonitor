package com.example.sleepmonitor.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sleepmonitor.data.local.SleepDatabase
import com.example.sleepmonitor.data.remote.BackendApiFactory
import com.example.sleepmonitor.data.remote.BackendSyncRepository
import com.example.sleepmonitor.data.repository.AuthRepository
import com.example.sleepmonitor.data.repository.RecommendationRepository
import com.example.sleepmonitor.data.repository.SleepRepository
import com.example.sleepmonitor.ui.auth.DeleteAccountViewModel
import com.example.sleepmonitor.ui.auth.ForgotPasswordViewModel
import com.example.sleepmonitor.ui.auth.LoginViewModel
import com.example.sleepmonitor.ui.auth.RegisterViewModel
import com.example.sleepmonitor.ui.profile.ProfileViewModel
import com.example.sleepmonitor.ui.recommendations.RecommendationsViewModel
import com.example.sleepmonitor.ui.sleep.SleepSessionViewModel
import com.example.sleepmonitor.ui.utils.SessionManager

class AppViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    private val database by lazy { SleepDatabase.getInstance(context.applicationContext) }
    private val backendSyncRepository by lazy {
        BackendSyncRepository(
            db = database,
            context = context.applicationContext,
            api = BackendApiFactory.create()
        )
    }
    private val sessionManager by lazy { SessionManager(context.applicationContext) }
    private val authRepository by lazy { AuthRepository(database, backendSyncRepository) }
    private val sleepRepository by lazy { SleepRepository(database, backendSyncRepository) }
    private val recommendationRepository by lazy { RecommendationRepository(database, backendSyncRepository) }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(authRepository, sessionManager) as T

            modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
                RegisterViewModel(authRepository, sessionManager) as T

            modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) ->
                ForgotPasswordViewModel(authRepository) as T

            modelClass.isAssignableFrom(DeleteAccountViewModel::class.java) ->
                DeleteAccountViewModel(authRepository, sessionManager) as T

            modelClass.isAssignableFrom(RecommendationsViewModel::class.java) ->
                RecommendationsViewModel(recommendationRepository, sessionManager) as T

            modelClass.isAssignableFrom(SleepSessionViewModel::class.java) ->
                SleepSessionViewModel(sleepRepository, sessionManager) as T

            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(authRepository, sessionManager) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
