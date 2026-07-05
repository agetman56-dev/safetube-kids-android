package ua.safetube.kids

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ua.safetube.kids.parental.ScreenPinHelper
import ua.safetube.kids.ui.CategoryScreen
import ua.safetube.kids.ui.ChannelScreen
import ua.safetube.kids.ui.HomeScreen
import ua.safetube.kids.ui.PlayerScreen
import ua.safetube.kids.ui.SettingsScreen
import ua.safetube.kids.ui.theme.SafeTubeKidsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContent {
            val appState = remember { AppState(applicationContext) }
            val navController = rememberNavController()

            SafeTubeKidsTheme {
                SafeTubeNavHost(
                    appState = appState,
                    navController = navController,
                    activity = this
                )
            }
        }
    }

    // Дитина не може вийти системною кнопкою "Назад" з приколотого (pinned) режиму —
    // системний диспетчер сам ігнорує onBackPressed під час lock task mode.
}

@androidx.compose.runtime.Composable
private fun SafeTubeNavHost(appState: AppState, navController: NavHostController, activity: Activity) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                appState = appState,
                onOpenCategory = { catIdx -> navController.navigate("category/$catIdx") },
                onOpenSettings = { navController.navigate("settings") },
                activity = activity
            )
        }
        composable("category/{catIdx}") { backStackEntry ->
            val catIdx = backStackEntry.arguments?.getString("catIdx")?.toIntOrNull() ?: 0
            CategoryScreen(
                appState = appState,
                catIndex = catIdx,
                onOpenChannel = { chIdx -> navController.navigate("channel/$catIdx/$chIdx") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("channel/{catIdx}/{chIdx}") { backStackEntry ->
            val catIdx = backStackEntry.arguments?.getString("catIdx")?.toIntOrNull() ?: 0
            val chIdx = backStackEntry.arguments?.getString("chIdx")?.toIntOrNull() ?: 0
            ChannelScreen(
                appState = appState,
                catIndex = catIdx,
                chIndex = chIdx,
                onOpenVideo = { videoId -> navController.navigate("player/$videoId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("player/{videoId}") { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId").orEmpty()
            PlayerScreen(
                appState = appState,
                videoId = videoId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                appState = appState,
                activity = activity,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
