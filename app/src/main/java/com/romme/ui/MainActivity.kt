package com.romme.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.romme.ui.screens.GameScreen
import com.romme.ui.screens.LobbyScreen
import com.romme.ui.screens.LoginScreen
import com.romme.ui.theme.RommeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RommeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = { navController.navigate("lobby") }
                            )
                        }
                        composable("lobby") {
                            LobbyScreen(
                                viewModel = viewModel,
                                onGameStarted = { navController.navigate("game") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("game") {
                            GameScreen(
                                viewModel = viewModel,
                                onGameEnd = {
                                    viewModel.resetGameState()
                                    navController.navigate("lobby")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
