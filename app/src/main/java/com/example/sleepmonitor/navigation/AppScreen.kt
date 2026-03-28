package com.example.sleepmonitor.navigation

sealed class AppScreen(
    val route: String,
    val label: String,
    val showInBottomBar: Boolean = false
) {
    data object Login : AppScreen("login", "Login")
    data object Register : AppScreen("register", "Registro")
    data object ForgotPassword : AppScreen("forgot_password", "Recuperar")
    data object Dashboard : AppScreen("dashboard", "Inicio", true)
    data object SleepSession : AppScreen("sleep_session", "Dormir", true)
    data object Recommendations : AppScreen("recommendations", "Consejos", true)
    data object Profile : AppScreen("profile", "Perfil", true)
    data object DeleteAccount : AppScreen("delete_account", "Eliminar")

    companion object {
        val bottomBarItems = listOf(Dashboard, SleepSession, Recommendations, Profile)
    }
}
