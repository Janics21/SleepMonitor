package com.example.sleepmonitor.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepmonitor.data.local.entities.UserEntity
import com.example.sleepmonitor.data.repository.AuthRepository
import com.example.sleepmonitor.data.repository.Result
import com.example.sleepmonitor.ui.utils.SessionManager
import com.example.sleepmonitor.ui.utils.ValidationUtils
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(emailOrUsername: String, password: String) {
        if (emailOrUsername.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Completa correo o usuario y contrasena")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            when (val result = repository.login(emailOrUsername.trim(), password)) {
                is Result.Success -> {
                    val token = java.util.UUID.randomUUID().toString()
                    sessionManager.saveSession(result.data.userId, token, result.data.username)
                    _loginState.value = LoginState.Success(result.data)
                }
                is Result.Error -> _loginState.value = LoginState.Error(result.message)
            }
        }
    }
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data class Success(val user: UserEntity) : LoginState()
    data class Error(val message: String) : LoginState()
}

class RegisterViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableLiveData<RegisterState>(RegisterState.Idle)
    val state: LiveData<RegisterState> = _state

    fun register(
        email: String,
        username: String,
        password: String,
        confirmPassword: String,
        peso: Int? = null,
        altura: Int? = null,
        sexo: String? = null,
        pais: String? = null,
        fechaNacimiento: Long? = null
    ) {
        when {
            !ValidationUtils.isValidEmail(email) ->
                _state.value = RegisterState.Error("El correo es invalido")

            !ValidationUtils.isValidUsername(username) ->
                _state.value = RegisterState.Error("El nombre de usuario debe tener al menos 3 caracteres")

            !ValidationUtils.isValidPassword(password) ->
                _state.value = RegisterState.Error("La contrasena debe tener al menos 8 caracteres y 1 numero")

            !ValidationUtils.passwordsMatch(password, confirmPassword) ->
                _state.value = RegisterState.Error("Las contrasenas no coinciden")

            else -> viewModelScope.launch {
                _state.value = RegisterState.Loading
                when (
                    val result = repository.register(
                        email = email.trim(),
                        username = username.trim(),
                        password = password,
                        peso = peso,
                        altura = altura,
                        sexo = sexo,
                        pais = pais,
                        fechaNacimiento = fechaNacimiento
                    )
                ) {
                    is Result.Success -> {
                        val token = java.util.UUID.randomUUID().toString()
                        sessionManager.saveSession(result.data.userId, token, result.data.username)
                        _state.value = RegisterState.Success
                    }
                    is Result.Error -> _state.value = RegisterState.Error(result.message)
                }
            }
        }
    }
}

sealed class RegisterState {
    data object Idle : RegisterState()
    data object Loading : RegisterState()
    data object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class ForgotPasswordViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _requestState = MutableLiveData<ForgotPasswordRequestState>(ForgotPasswordRequestState.Idle)
    val requestState: LiveData<ForgotPasswordRequestState> = _requestState

    private val _resetState = MutableLiveData<ResetPasswordState>(ResetPasswordState.Idle)
    val resetState: LiveData<ResetPasswordState> = _resetState

    fun requestReset(email: String) {
        if (!ValidationUtils.isValidEmail(email)) {
            _requestState.value = ForgotPasswordRequestState.Error("El correo es invalido")
            return
        }

        viewModelScope.launch {
            _requestState.value = ForgotPasswordRequestState.Loading
            when (val result = repository.requestPasswordReset(email.trim())) {
                is Result.Success -> {
                    val token = result.data.takeUnless { it == "demo-hidden" }
                    _requestState.value = ForgotPasswordRequestState.EmailPrepared(localDemoToken = token)
                }
                is Result.Error -> _requestState.value = ForgotPasswordRequestState.Error(result.message)
            }
        }
    }

    fun resetPassword(token: String, newPassword: String, confirmPassword: String) {
        when {
            token.isBlank() -> _resetState.value = ResetPasswordState.Error("Introduce el codigo o enlace temporal")
            !ValidationUtils.isValidPassword(newPassword) ->
                _resetState.value = ResetPasswordState.Error("La contrasena debe tener al menos 8 caracteres y 1 numero")

            !ValidationUtils.passwordsMatch(newPassword, confirmPassword) ->
                _resetState.value = ResetPasswordState.Error("Las contrasenas no coinciden")

            else -> viewModelScope.launch {
                _resetState.value = ResetPasswordState.Loading
                when (val result = repository.resetPassword(token.trim(), newPassword)) {
                    is Result.Success -> _resetState.value = ResetPasswordState.Success
                    is Result.Error -> _resetState.value = ResetPasswordState.Error(result.message)
                }
            }
        }
    }
}

sealed class ForgotPasswordRequestState {
    data object Idle : ForgotPasswordRequestState()
    data object Loading : ForgotPasswordRequestState()
    data class EmailPrepared(val localDemoToken: String?) : ForgotPasswordRequestState()
    data class Error(val message: String) : ForgotPasswordRequestState()
}

sealed class ResetPasswordState {
    data object Idle : ResetPasswordState()
    data object Loading : ResetPasswordState()
    data object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}

class DeleteAccountViewModel(
    private val repository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableLiveData<DeleteState>(DeleteState.Idle)
    val state: LiveData<DeleteState> = _state

    fun deleteAccount(password: String) {
        val userId = sessionManager.getUserId()
        if (userId.isNullOrBlank()) {
            _state.value = DeleteState.Error("Sesion no valida")
            return
        }

        viewModelScope.launch {
            _state.value = DeleteState.Loading
            when (val result = repository.deleteAccount(userId, password)) {
                is Result.Success -> {
                    sessionManager.clearSession()
                    _state.value = DeleteState.Success
                }
                is Result.Error -> _state.value = DeleteState.Error(result.message)
            }
        }
    }
}

sealed class DeleteState {
    data object Idle : DeleteState()
    data object Loading : DeleteState()
    data object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}
