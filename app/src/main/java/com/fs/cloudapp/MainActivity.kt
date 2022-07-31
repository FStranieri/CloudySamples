package com.fs.cloudapp

import android.R.attr
import android.content.Intent
import android.os.Bundle
import android.support.annotation.NonNull
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.fs.cloudapp.composables.BindAccounts
import com.fs.cloudapp.composables.BindChat
import com.fs.cloudapp.data.user_push_tokens
import com.fs.cloudapp.viewmodels.AuthViewModel
import com.fs.cloudapp.viewmodels.CloudDBViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
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

            val loggedIn = remember { authViewModel.loggedIn }.value
            val googleLogin = remember { googleLoginToken }.value
            val registerPushToken = remember { cloudDBViewModel.canRegisterPushToken }.value

            if (googleLogin.isNotEmpty()) {
                authViewModel.loginWithCredentials(
                    GoogleAuthProvider.credentialWithToken(googleLoginToken.value))
            }

            if (loggedIn) {
                cloudDBViewModel.initAGConnectCloudDB(
                    this,
                    authViewModel.authInstance,
                    authViewModel.getOutput().value!!
                )

                if (registerPushToken) {
                    getPushToken(cloudDBViewModel)

                    BindChat(cloudDBViewModel = cloudDBViewModel)

                    cloudDBViewModel.getAllMessages()
                }
            } else {
                BindAccounts(authViewModel = authViewModel, this)
            }

            DisposableEffect(key1 = cloudDBViewModel) {
                onDispose {
                    //cloudDBViewModel.closeDB()
                }
            }
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