package com.fs.cloudapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.fs.cloudapp.composables.BindAccountScreen
import com.fs.cloudapp.composables.ChatScreen
import com.fs.cloudapp.data.user_push_tokens
import com.fs.cloudapp.viewmodels.AuthViewModel
import com.fs.cloudapp.viewmodels.CloudDBViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.huawei.agconnect.api.AGConnectApi
import com.huawei.agconnect.auth.GoogleAuthProvider
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException


class MainActivity : ComponentActivity() {

    private var googleLoginToken: MutableState<String> = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val authViewModel: AuthViewModel by viewModels()
            val cloudDBViewModel: CloudDBViewModel by viewModels()
            val authState by authViewModel.state.collectAsState()
            val cloudState by cloudDBViewModel.state.collectAsState()

            val googleLogin = remember { googleLoginToken }.value

            authState.failureOutput?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                authViewModel.resetFailureOutput()
            }

            cloudState.failureOutput?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                cloudDBViewModel.resetFailureOutput()
            }

            //method 2 for google login, disabled by default
            if (googleLogin.isNotEmpty()) {
                authViewModel.loginWithCredentials(
                    GoogleAuthProvider.credentialWithToken(googleLoginToken.value))
            }

            //if loggedIn, initialize the CloudDB instance with the user credentials
            if (authState.loggedIn) {
                cloudDBViewModel.initAGConnectCloudDB(
                    this,
                    authViewModel.authInstance
                )

                //if the db is initialized continue with the flow
                if (cloudState.dbReady) {
                    //store the user credentials ONLY if it's the very 1st login
                    if(!authState.previousInstanceAlive) {
                        cloudDBViewModel.saveUser(authViewModel.authInstance.currentUser)
                        getPushToken(cloudDBViewModel)
                    }

                    //compose the Chat screen
                    ChatScreen(authViewModel= authViewModel, cloudDBViewModel = cloudDBViewModel)

                    //get all messages only for the 1st time, then a listener will be registered
                    cloudDBViewModel.getAllMessages()
                }
            } else {
                BindAccountScreen(authViewModel = authViewModel)
            }

            /*DisposableEffect(key1 = cloudDBViewModel) {
                onDispose {
                    //cloudDBViewModel.closeDB()
                }
            }*/
        }
    }

    private fun getPushToken(cloudDBViewModel: CloudDBViewModel) {
        // Create a thread.
        object : Thread() {
            override fun run() {
                try {
                    // Obtain the app ID from the agconnect-services.json file.
                    val appId = getString(R.string.app_id)
                    val token = HmsInstanceId.getInstance(this@MainActivity).getToken(appId, "HCM")
                    Log.i(TAG, "get token:$token")

                    // Check whether the token is null.
                    if (token?.isNotEmpty() == true) {
                        sendRegTokenToServer(token, cloudDBViewModel)
                    }
                } catch (e: ApiException) {
                    Log.e(TAG, "get token failed, $e")
                }
            }
        }.start()
    }

    private fun sendRegTokenToServer(token: String, cloudDBViewModel: CloudDBViewModel) {
        Log.i(TAG, "sending token to server. token:$token")
        cloudDBViewModel.savePushToken(user_push_tokens().apply {
            setToken(token)
            user_id = cloudDBViewModel.userID
            platform = 0
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode === AuthViewModel.GOOGLE_SIGN_IN) {
            val task: Task<GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            task.addOnSuccessListener { googleSignInAccount ->
                googleLoginToken.value = googleSignInAccount!!.idToken!!
            }.addOnFailureListener {
                Log.e(TAG, "error: ${it.message}")
            }
        } else {
            AGConnectApi.getInstance().activityLifecycle()
                .onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}