package com.example.logincompose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.logincompose.models.SignInViewModel
import com.example.logincompose.screens.ProfileScreen
import com.example.logincompose.screens.SignInScreen
import com.example.logincompose.ui.theme.LoginComposeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val googleAuthClient by lazy {
        GoogleAuthClient(
            context = applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onPrimary
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "sign_in"
                    ) {
                        composable("sign_in") {
                            val viewModel = viewModel<SignInViewModel>()
                            val state by viewModel.state.collectAsState()

                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartIntentSenderForResult(),
                                onResult = { result ->
                                    if (result.resultCode == RESULT_OK) {
                                        lifecycleScope.launch {
                                            val signInResult =
                                                googleAuthClient.getSignInUserInfo(
                                                    intent = result.data ?: return@launch
                                                )
                                            viewModel.onSignInResult(signInResult)
                                        }
                                    }
                                }
                            )

                            SignInScreen(state = state) {
                                lifecycleScope.launch {
                                    val signInIntentSender = googleAuthClient.signIn()
                                    launcher.launch(
                                        IntentSenderRequest.Builder(
                                            signInIntentSender ?: return@launch
                                        ).build()
                                    )
                                    navController.navigate("profile")
                                }
                            }

                            LaunchedEffect(key1 = state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign in successful",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    viewModel.resetState()
                                }
                            }
                        }
                        composable("profile") {
                            ProfileScreen(userData = googleAuthClient.getSignedInUser()) {
                                lifecycleScope.launch {
                                    googleAuthClient.signOut()
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign out",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}
