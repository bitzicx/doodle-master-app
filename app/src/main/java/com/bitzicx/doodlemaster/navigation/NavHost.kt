package com.bitzicx.doodlemaster.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bitzicx.doodlemaster.classic_game_screen.GameRoomScreen
import com.bitzicx.doodlemaster.main_screen.MainScreen
import com.bitzicx.doodlemaster.profile_screen.ProfileScreen
import com.bitzicx.doodlemaster.classic_game_screen.WaitingRoomScreen
import com.bitzicx.doodlemaster.howto.HowToScreen
import com.bitzicx.doodlemaster.leaderboard.LeaderBoardScreen
import com.bitzicx.settings.SettingsScreen


sealed class Screen(val route: String){
    object MainScreen: Screen("mainScreen")
    object Settings: Screen("settings")
    object Profile: Screen("profile")
    object Leaderboard: Screen("leaderboard")
    object HowTo: Screen("howto")

    object WaitingRoom: Screen("waitingRoom")

    object GameScreen: Screen("gameScreen")
}

@Composable
fun AppNavigation(){
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.MainScreen.route){

        composable(Screen.MainScreen.route) {
            MainScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },

                onNavigateToLeaderboard = {
                    navController.navigate(Screen.Leaderboard.route)
                },

                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },

                onNavigateToHowTo = {
                    navController.navigate(Screen.HowTo.route)
                },

                onNavigateToGameScreen = {
                    navController.navigate(Screen.GameScreen.route)
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(Screen.Leaderboard.route) {
            LeaderBoardScreen()
        }

        composable(Screen.HowTo.route) {
            HowToScreen()
        }

        composable(Screen.GameScreen.route){
            GameRoomScreen(
                onNavigateToMainScreen = {
                    navController.navigate(Screen.MainScreen.route) {
                        navController.popBackStack(Screen.MainScreen.route, inclusive = false)                    }
                }
            )
        }

    }

}