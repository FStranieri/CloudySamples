package com.fs.cloudapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.fs.cloudapp.composables.LoginScreen
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

/**
This is the activity including all the flows starting from the authentication flow
up to the chat flow
 **/
class MainActivity : ComponentActivity() {

    //Google login token with the METHOD 2
    private var googleLoginToken: MutableState<String> = mutableStateOf("")

    //Google login activity result with METHOD 2
    private val googleLoginIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task: Task<GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
            task.addOnSuccessListener { googleSignInAccount ->
                googleLoginToken.value = googleSignInAccount!!.idToken!!
            }.addOnFailureListener {
                Log.e(TAG, "error: ${it.message}")
            }
        } else {
            AGConnectApi.getInstance()
                .activityLifecycle()
                .onActivityResult(
                    AuthViewModel.GOOGLE_SIGN_IN,
                    result.resultCode,
                    result.data
                )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val authViewModel: AuthViewModel by viewModels()
            val cloudDBViewModel: CloudDBViewModel by viewModels()

            //listen for authentication flow updates
            val authState by authViewModel.state.collectAsState()

            //listen for cloud DB status updates
            val cloudState by cloudDBViewModel.state.collectAsState()

            //listen for google login flow updates with METHOD 2
            val googleToken = remember { googleLoginToken }.value

            //listen for authentication flow errors
            authState.failureOutput?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                authViewModel.resetFailureOutput()
            }

            //listen for cloud db status errors
            cloudState.failureOutput?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                cloudDBViewModel.resetFailureOutput()
            }

            //method 2 for login, disabled by default, just as example of usage
            if (googleToken.isNotEmpty()) {
                authViewModel.loginWithCredentials(
                    GoogleAuthProvider.credentialWithToken(googleToken))
                googleLoginToken.value = ""
            }

            //if loggedIn, initialize the CloudDB instance with the user credentials
            if (authState.loggedIn) {
                cloudDBViewModel.initAGConnectCloudDB(
                    this,
                    authViewModel.authInstance
                )

                //if the db is initialized continue with the flow
                if (cloudState.dbReady) {
                    if (!cloudState.userDataAvailable) {
                        cloudDBViewModel.getUserDataAvailability()
                    } else {
                        //store the user credentials ONLY if it's the very 1st login
                        if (!authState.previousInstanceAlive) {
                            getPushToken(cloudDBViewModel)
                        }

                        //compose the Chat screen
                        ChatScreen(
                            authViewModel = authViewModel,
                            cloudDBViewModel = cloudDBViewModel
                        )

                        //get all messages only for the 1st time, then a listener will be registered
                        cloudDBViewModel.getAllMessages()
                    }
                }
            } else {
                // screen with the login options
                LoginScreen(authViewModel = authViewModel, googleLoginIntentLauncher)
            }
        }
    }

    // Register the push token for push notifications
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

    // Save the token into a Cloud DB Object Type
    private fun sendRegTokenToServer(token: String, cloudDBViewModel: CloudDBViewModel) {
        Log.i(TAG, "sending token to server. token:$token")
        cloudDBViewModel.savePushToken(user_push_tokens().apply {
            setToken(token)
            user_id = cloudDBViewModel.userID
            platform = 0
        })
    }

    companion object {
        const val TAG = "MainActivity"
    }
}