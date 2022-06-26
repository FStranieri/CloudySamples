package com.fs.cloudapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fs.cloudapp.data.user_push_tokens
import com.fs.cloudapp.ui.theme.CloudAppTheme
import com.fs.cloudapp.viewmodels.CloudDBViewModel
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cloudDBViewModel: CloudDBViewModel by viewModels()
            cloudDBViewModel.initAGConnectCloudDB(this)

            val registerPushToken = remember { cloudDBViewModel.canRegisterPushToken }.value

            CloudAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("Android")
                }
            }

            if (registerPushToken) {
                getPushToken(cloudDBViewModel)
            }
        }
    }

    private fun getPushToken(cloudDBViewModel: CloudDBViewModel) {
        // Create a thread.
        object : Thread() {
            override fun run() {
                try {
                    // Obtain the app ID from the agconnect-services.json file.
                    val appId = "106551957"
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
            user_id = "Fra_NOVA"
            platform = 0
        })
    }

    companion object {
        const val TAG = "MainActivity"
    }
}


@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CloudAppTheme {
        Greeting("Android")
    }
}