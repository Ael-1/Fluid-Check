package com.example.fluidcheck.ui.navigation

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object Progress : NavRoutes("progress")
    object AICoach : NavRoutes("ai_coach")
    object Settings : NavRoutes("settings")
    object Admin : NavRoutes("admin")
    object SignUp : NavRoutes("signup")
    object EditProfile : NavRoutes("edit_profile")
    object AboutDeveloper : NavRoutes("about_developer")
}