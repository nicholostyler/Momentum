package com.nicholostyler.momentum

import android.R.attr.onClick
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.nicholostyler.momentum.repository.TodoRepository
import com.nicholostyler.momentum.sync.SyncManager
import com.nicholostyler.momentum.ui.theme.MomentumTheme
import com.nicholostyler.momentum.viewmodel.TodoViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.nicholostyler.momentum.data.model.TodoItem
import com.nicholostyler.momentum.ui.home.AddTodoBottomSheet
import com.nicholostyler.momentum.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@HiltAndroidApp
class MomentumApplication : Application(){

}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MomentumTheme {
                val navController = rememberNavController()
                val auth = Firebase.auth
                val isLoggedIn = remember(auth.currentUser) {
                    auth.currentUser != null
                }
                val viewModel: TodoViewModel = hiltViewModel()


                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val coroutineScope = rememberCoroutineScope()
                var showSheet by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Momentum") },
                            actions = {
                                if (isLoggedIn) {
                                    SignOutButton {
                                        auth.signOut()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                ) { innerPadding ->
                    Box(Modifier.padding(top = innerPadding.calculateTopPadding())) {
                        AppNavGraph(
                            navController = navController,
                            isLoggedIn = isLoggedIn,
                            modifier = Modifier.fillMaxSize(),
                            todoViewModel = viewModel
                        )
                    }

                    if (showSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showSheet = false },
                            sheetState = sheetState,
                            dragHandle = null
                        ) {
                            AddTodoBottomSheet(

                                onAdd = { title, dueDate ->
                                    val userId = auth.currentUser?.uid ?: return@AddTodoBottomSheet
                                    val todo = TodoItem(
                                        title = title,
                                        dueDate = dueDate,
                                        completed = false
                                    )
                                    coroutineScope.launch {
                                        viewModel.addTodo(userId, todo)
                                    }
                                    showSheet = false
                                },
                                onDismiss = { showSheet = false },

                            )
                        }

                    }


                }
            }
        }
    }

    @Composable
    fun SignOutButton(onSignOut: () -> Unit) {
        TextButton(onClick = onSignOut) {
            Text("Sign Out")
        }
    }


    @Composable
    fun AuthStatusBanner() {
        val user = Firebase.auth.currentUser

        val message = if (user != null) {
            "Signed in as ${user.email ?: "Anonymous"}"
        } else {
            "Not signed in"
        }

        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }


    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        MomentumTheme {
            Greeting("Android")
        }
    }
}