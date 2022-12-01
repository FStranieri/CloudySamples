package com.fs.cloudapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.fs.cloudapp.composables.ChatScreen
import com.fs.cloudapp.composables.LoginScreen
import com.fs.cloudapp.repositories.AuthRepository
import com.fs.cloudapp.repositories.CloudDBRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.huawei.agconnect.api.AGConnectApi
import com.huawei.agconnect.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
This is the activity including all the flows starting from the authentication flow
up to the chat flow
 **/
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModelFactory }

    private val googleSignInResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onGoogleSignInLaunchResult(it)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val authState by viewModel.loginState.collectAsState()

            when (authState) {
                is AuthRepository.AuthState.LoggedIn -> {
                    ChatScreen(viewModel.cloudDBRepository, viewModel.authRepository)
                }
                AuthRepository.AuthState.LoggedOut -> {
                    // screen with the login options
                    LoginScreen(googleSignInResultLauncher, viewModel.authRepository)
                }
            }
        }
    }


    //TODO: remove it with the new Auth Service version
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Call to deprecated onActivityResult")
        AGConnectApi.getInstance().activityLifecycle()
            .onActivityResult(requestCode, resultCode, data)
    }
}

class MainViewModel(
    application: MainApplication
) : ViewModel() {

    val authRepository = application.authRepository
    val cloudDBRepository = application.cloudDBRepository
    private val pushTokenRepository = application.pushTokenRepository

    val loginState = authRepository.observeLoginState()

    private val genericExceptionHandler =
        CoroutineExceptionHandler { _, e -> Log.e(this.TAG, e.toString()) }

    init {
        viewModelScope.launch(genericExceptionHandler) {
            // at every successful login, fetch a push token and send it to the cloud
            authRepository.observeLoginState().filter {
                it is AuthRepository.AuthState.LoggedIn
            }.collect {
                savePushTokenToCloud()
            }
        }
    }

    private suspend fun savePushTokenToCloud() {
        val pushToken = pushTokenRepository.getPushToken()
        cloudDBRepository.observeConnectionState()
            .first { it is CloudDBRepository.CloudState.Connected }
        cloudDBRepository.savePushToken(pushToken)
    }

    fun onGoogleSignInLaunchResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Google direct SignIn result OK -> proceed with token")
            viewModelScope.launch(genericExceptionHandler) {
                val googleSignInToken =
                    GoogleSignIn.getSignedInAccountFromIntent(result.data).await().idToken
                val googleCredentials = GoogleAuthProvider.credentialWithToken(googleSignInToken)
                authRepository.loginWithCredentials(googleCredentials)
            }
        } else {
            Log.d(TAG, "Google direct SignIn result failed -> proceed with AGC")
            AGConnectApi.getInstance()
                .activityLifecycle()
                .onActivityResult(
                    AuthRepository.GOOGLE_SIGN_IN,
                    result.resultCode,
                    result.data
                )
        }
    }


}

object MainViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[APPLICATION_KEY]) as MainApplication
        return MainViewModel(application) as T
    }
}



