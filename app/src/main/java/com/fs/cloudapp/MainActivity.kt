package com.fs.cloudapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import com.fs.cloudapp.composables.BindChat
import com.fs.cloudapp.data.user_push_tokens
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

            if (registerPushToken) {
                getPushToken(cloudDBViewModel)

                BindChat(cloudDBViewModel = cloudDBViewModel)

                cloudDBViewModel.getAllMessages()
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