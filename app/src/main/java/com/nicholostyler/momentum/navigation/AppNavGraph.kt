package com.nicholostyler.momentum.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nicholostyler.momentum.ui.auth.LoginScreen
import com.nicholostyler.momentum.ui.home.HomeScreen
import com.nicholostyler.momentum.viewmodel.TodoViewModel

@Composable
fun AppNavGraph(navController: NavHostController, isLoggedIn: Boolean, modifier: Modifier, todoViewModel: TodoViewModel) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                modifier = modifier,
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true } // clear backstack
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(viewModel = todoViewModel)
        }
    }
}
